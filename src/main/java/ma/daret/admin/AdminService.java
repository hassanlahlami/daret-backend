package ma.daret.admin;

import ma.daret.admin.AdminDtos.AdminCircleView;
import ma.daret.admin.AdminDtos.AdminUserView;
import ma.daret.admin.AdminDtos.Overview;
import ma.daret.circles.Circle;
import ma.daret.circles.CircleMemberRepository;
import ma.daret.circles.CircleRepository;
import ma.daret.circles.CircleStatus;
import ma.daret.circles.SlotRepository;
import ma.daret.common.web.ApiException;
import ma.daret.compliance.Audited;
import ma.daret.identity.kyc.KycProfile;
import ma.daret.identity.kyc.KycProfileRepository;
import ma.daret.identity.user.User;
import ma.daret.identity.user.UserRepository;
import ma.daret.ledger.LedgerAccounts;
import ma.daret.ledger.LedgerService;
import ma.daret.identity.user.SystemUsers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private final CircleRepository circles;
    private final CircleMemberRepository members;
    private final SlotRepository slots;
    private final UserRepository users;
    private final KycProfileRepository kycProfiles;
    private final LedgerService ledger;
    private final ma.daret.circles.ContributionRepository contributions;
    private final ma.daret.circles.PayoutRepository payouts;
    private final ma.daret.compliance.DisputeRepository disputes;
    private final ma.daret.compliance.AmlFlagRepository amlFlags;
    private final ma.daret.social.VouchRepository vouches;

    public AdminService(CircleRepository circles,
                        CircleMemberRepository members,
                        SlotRepository slots,
                        UserRepository users,
                        KycProfileRepository kycProfiles,
                        LedgerService ledger,
                        ma.daret.circles.ContributionRepository contributions,
                        ma.daret.circles.PayoutRepository payouts,
                        ma.daret.compliance.DisputeRepository disputes,
                        ma.daret.compliance.AmlFlagRepository amlFlags,
                        ma.daret.social.VouchRepository vouches) {
        this.circles = circles;
        this.members = members;
        this.slots = slots;
        this.users = users;
        this.kycProfiles = kycProfiles;
        this.ledger = ledger;
        this.contributions = contributions;
        this.payouts = payouts;
        this.disputes = disputes;
        this.amlFlags = amlFlags;
        this.vouches = vouches;
    }

    @Transactional(readOnly = true)
    public AdminDtos.Analytics analytics() {
        Map<String, Long> contribByStatus = new LinkedHashMap<>();
        for (ma.daret.circles.Contribution.Status s : ma.daret.circles.Contribution.Status.values()) {
            contribByStatus.put(s.name(), contributions.countByStatus(s));
        }
        Map<String, Long> payoutByStatus = new LinkedHashMap<>();
        for (ma.daret.circles.Payout.Status s : ma.daret.circles.Payout.Status.values()) {
            payoutByStatus.put(s.name(), payouts.countByStatus(s));
        }

        // New circles per week over the last 8 weeks (oldest → newest).
        java.time.Instant now = java.time.Instant.now();
        long[] buckets = new long[8];
        for (Circle c : circles.findByCreatedAtAfter(now.minus(56, java.time.temporal.ChronoUnit.DAYS))) {
            long weeksAgo = java.time.Duration.between(c.getCreatedAt(), now).toDays() / 7;
            if (weeksAgo >= 0 && weeksAgo < 8) {
                buckets[(int) (7 - weeksAgo)]++;
            }
        }
        java.util.List<AdminDtos.WeeklyPoint> series = new java.util.ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            series.add(new AdminDtos.WeeklyPoint("S" + (i - 7), buckets[i]));
        }

        return new AdminDtos.Analytics(contribByStatus, payoutByStatus,
                disputes.countByStatus(ma.daret.compliance.Dispute.Status.OPEN),
                amlFlags.countByStatus(ma.daret.compliance.AmlFlag.Status.OPEN),
                vouches.count(),
                series);
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (CircleStatus s : CircleStatus.values()) {
            byStatus.put(s.name(), circles.countByStatus(s));
        }
        return new Overview(
                byStatus,
                users.count(),
                ledger.balanceOf(LedgerAccounts.PLATFORM_REVENUE),
                ledger.balanceOf(LedgerAccounts.member(SystemUsers.PLATFORM_USER_ID)),
                slots.countByPlatformFunded(true)
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminCircleView> circles(CircleStatus status, Pageable pageable) {
        Page<Circle> page = status != null
                ? circles.findByStatus(status, pageable)
                : circles.findAll(pageable);
        return page.map(c -> new AdminCircleView(c.getId(), c.getName(), c.getStatus(),
                c.getSeatCount(), members.countByCircleId(c.getId()), c.getContributionAmount(),
                c.isShariaMode(), c.getOwnerId(), c.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public Page<AdminUserView> users(Pageable pageable) {
        return users.findAll(pageable).map(this::toView);
    }

    /** Enable or disable a user's account (audited). */
    @Audited("user.set-enabled")
    @Transactional
    public AdminUserView setEnabled(UUID userId, boolean enabled) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        user.setEnabled(enabled);
        return toView(users.save(user));
    }

    private AdminUserView toView(User user) {
        var kyc = kycProfiles.findByUserId(user.getId()).map(KycProfile::getStatus).orElse(null);
        return new AdminUserView(user.getId(), user.getEmail(), user.getPhone(),
                user.getRoles(), user.isEnabled(), kyc);
    }
}
