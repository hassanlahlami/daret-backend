package ma.daret.social;

import ma.daret.common.web.ApiException;
import ma.daret.compliance.Audited;
import ma.daret.identity.kyc.KycGate;
import ma.daret.scoring.ScoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Peer vouching (spec §19). A verified member vouches for another; the vouch is a
 * trust signal that lifts the vouchee's credit score.
 */
@Service
public class VouchService {

    private final VouchRepository vouches;
    private final KycGate kycGate;
    private final ScoringService scoring;

    public VouchService(VouchRepository vouches, KycGate kycGate, ScoringService scoring) {
        this.vouches = vouches;
        this.kycGate = kycGate;
        this.scoring = scoring;
    }

    @Audited("social.vouch")
    @Transactional
    public void vouch(UUID voucherUserId, UUID voucheeUserId) {
        if (voucherUserId.equals(voucheeUserId)) {
            throw ApiException.badRequest("CANNOT_VOUCH_SELF", "You cannot vouch for yourself");
        }
        kycGate.requireVerified(voucherUserId);
        if (vouches.existsByVoucherUserIdAndVoucheeUserId(voucherUserId, voucheeUserId)) {
            throw ApiException.conflict("ALREADY_VOUCHED", "You already vouched for this member");
        }
        vouches.save(new Vouch(voucherUserId, voucheeUserId));
        // Refresh the vouchee's score so the trust signal takes effect immediately.
        scoring.recompute(voucheeUserId);
    }

    @Transactional(readOnly = true)
    public long vouchesFor(UUID userId) {
        return vouches.countByVoucheeUserId(userId);
    }
}
