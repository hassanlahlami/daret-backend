package ma.daret.social;

import ma.daret.common.web.ApiException;
import ma.daret.compliance.Audited;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/** Invite codes for private (invite-only) circles. */
@Service
public class InviteService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final CircleInviteRepository invites;

    public InviteService(CircleInviteRepository invites) {
        this.invites = invites;
    }

    @Audited("circle.invite")
    @Transactional
    public CircleInvite create(UUID circleId, UUID inviterUserId) {
        return invites.save(new CircleInvite(newCode(), circleId, inviterUserId));
    }

    /**
     * Validate an invite code for a circle and consume it (mark USED). Throws if the
     * code is missing, for another circle, or already used/revoked.
     */
    @Transactional
    public void consume(UUID circleId, String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("INVITE_REQUIRED", "This circle requires an invite code");
        }
        CircleInvite invite = invites.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> ApiException.badRequest("INVALID_INVITE", "Invite code not recognized"));
        if (!invite.getCircleId().equals(circleId) || invite.getStatus() != CircleInvite.Status.ACTIVE) {
            throw ApiException.badRequest("INVALID_INVITE", "Invite code is not valid for this circle");
        }
        invite.setStatus(CircleInvite.Status.USED);
        invites.save(invite);
    }

    private static String newCode() {
        StringBuilder sb = new StringBuilder("DRT-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
