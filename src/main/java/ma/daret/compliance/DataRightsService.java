package ma.daret.compliance;

import ma.daret.circles.CircleMember;
import ma.daret.circles.CircleMemberRepository;
import ma.daret.circles.CircleRepository;
import ma.daret.circles.Contribution;
import ma.daret.circles.ContributionRepository;
import ma.daret.circles.Payout;
import ma.daret.circles.PayoutRepository;
import ma.daret.common.web.ApiException;
import ma.daret.identity.kyc.KycProfile;
import ma.daret.identity.kyc.KycProfileRepository;
import ma.daret.identity.user.User;
import ma.daret.identity.user.UserRepository;
import ma.daret.ledger.LedgerAccounts;
import ma.daret.ledger.LedgerEntryRepository;
import ma.daret.scoring.CreditScore;
import ma.daret.scoring.ScoringService;
import ma.daret.social.VouchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * CNDP data rights (spec §13): right of access (a live self-service export of
 * the member's own personal data, deliberately UN-masked because the subject is
 * entitled to it) and right to erasure (a reviewable request, since money and
 * ledger records must be retained while obligations remain).
 */
@Service
public class DataRightsService {

    private final UserRepository users;
    private final KycProfileRepository kycProfiles;
    private final CircleMemberRepository memberships;
    private final CircleRepository circles;
    private final ContributionRepository contributions;
    private final PayoutRepository payouts;
    private final LedgerEntryRepository ledger;
    private final VouchRepository vouches;
    private final DisputeRepository disputes;
    private final ScoringService scoring;
    private final ErasureRequestRepository erasures;

    public DataRightsService(UserRepository users, KycProfileRepository kycProfiles,
                             CircleMemberRepository memberships, CircleRepository circles,
                             ContributionRepository contributions, PayoutRepository payouts,
                             LedgerEntryRepository ledger, VouchRepository vouches,
                             DisputeRepository disputes, ScoringService scoring,
                             ErasureRequestRepository erasures) {
        this.users = users;
        this.kycProfiles = kycProfiles;
        this.memberships = memberships;
        this.circles = circles;
        this.contributions = contributions;
        this.payouts = payouts;
        this.ledger = ledger;
        this.vouches = vouches;
        this.disputes = disputes;
        this.scoring = scoring;
        this.erasures = erasures;
    }

    // ---- Export payload (raw, un-masked: this is the subject's own data) ----

    public record ProfileExport(UUID userId, String email, String phone, List<String> roles,
                                boolean enabled, Instant createdAt) {
    }

    public record KycExport(String status, String partnerRef, Instant consentedAt) {
    }

    public record ScoreExport(int score, String tier) {
    }

    public record MembershipExport(UUID circleId, String circleName, String slotPreference,
                                   Integer slotPosition, String standing) {
    }

    public record ContributionExport(UUID circleId, int cycleIndex, BigDecimal amount,
                                     LocalDate dueDate, String status) {
    }

    public record PayoutExport(UUID circleId, int cycleIndex, BigDecimal amount, String status) {
    }

    public record DisputeExport(UUID circleId, String subject, String status, Instant at) {
    }

    public record DataExport(ProfileExport profile, KycExport kyc, ScoreExport score,
                             List<MembershipExport> memberships, List<ContributionExport> contributions,
                             List<PayoutExport> payouts, BigDecimal ledgerBalance, long vouchesReceived,
                             List<DisputeExport> disputes, Instant generatedAt) {
    }

    @Audited("data.export")
    @Transactional(readOnly = true)
    public DataExport export(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        ProfileExport profile = new ProfileExport(
                user.getId(), user.getEmail(), user.getPhone(),
                user.getRoles().stream().map(Enum::name).sorted().toList(),
                user.isEnabled(), user.getCreatedAt());

        KycExport kyc = kycProfiles.findByUserId(userId)
                .map(this::toKycExport)
                .orElse(new KycExport("NONE", null, null));

        CreditScore cs = scoring.getOrCompute(userId);
        ScoreExport score = new ScoreExport(cs.getScore(), cs.getTier().name());

        List<MembershipExport> ms = memberships.findByUserId(userId).stream()
                .map(this::toMembershipExport)
                .toList();

        List<ContributionExport> cs2 = contributions.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Contribution::getDueDate))
                .map(c -> new ContributionExport(c.getCircleId(), c.getCycleIndex(), c.getAmount(),
                        c.getDueDate(), c.getStatus().name()))
                .toList();

        List<PayoutExport> ps = payouts.findByRecipientUserId(userId).stream()
                .map(p -> new PayoutExport(p.getCircleId(), p.getCycleIndex(), p.getAmount(), p.getStatus().name()))
                .toList();

        BigDecimal balance = ledger.balanceOf(LedgerAccounts.member(userId));

        List<DisputeExport> ds = disputes.findByRaiserUserIdOrderByCreatedAtDesc(userId).stream()
                .map(d -> new DisputeExport(d.getCircleId(), d.getSubject(), d.getStatus().name(), d.getCreatedAt()))
                .toList();

        return new DataExport(profile, kyc, score, ms, cs2, ps, balance,
                vouches.countByVoucheeUserId(userId), ds, Instant.now());
    }

    private KycExport toKycExport(KycProfile k) {
        return new KycExport(k.getStatus().name(), k.getPartnerRef(), k.getConsentedAt());
    }

    private MembershipExport toMembershipExport(CircleMember m) {
        String name = circles.findById(m.getCircleId()).map(c -> c.getName()).orElse(null);
        return new MembershipExport(m.getCircleId(), name, m.getSlotPreference().name(),
                m.getSlotPosition(), m.getStanding().name());
    }

    // ---- Erasure requests ----

    @Audited("data.erasure-request")
    @Transactional
    public ErasureRequest requestErasure(UUID userId, String reason) {
        return erasures.save(new ErasureRequest(userId, reason == null ? null : reason.trim()));
    }

    @Transactional(readOnly = true)
    public List<ErasureRequest> mine(UUID userId) {
        return erasures.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<ErasureRequest> pending() {
        return erasures.findByStatusOrderByCreatedAtDesc(ErasureRequest.Status.PENDING);
    }

    @Audited("data.erasure-resolve")
    @Transactional
    public ErasureRequest resolve(UUID id, boolean approved, String resolution) {
        ErasureRequest r = erasures.findById(id)
                .orElseThrow(() -> ApiException.notFound("ERASURE_NOT_FOUND", "Erasure request not found"));
        r.setStatus(approved ? ErasureRequest.Status.APPROVED : ErasureRequest.Status.REJECTED);
        r.setResolution(resolution);
        return erasures.save(r);
    }
}
