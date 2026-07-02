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

    public AdminService(CircleRepository circles,
                        CircleMemberRepository members,
                        SlotRepository slots,
                        UserRepository users,
                        KycProfileRepository kycProfiles,
                        LedgerService ledger) {
        this.circles = circles;
        this.members = members;
        this.slots = slots;
        this.users = users;
        this.kycProfiles = kycProfiles;
        this.ledger = ledger;
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
