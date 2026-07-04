package ma.daret.circles;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request/response payloads for the circles module. */
public final class CircleDtos {

    private CircleDtos() {
    }

    public record CreateCircleRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull @DecimalMin(value = "1.0", message = "contributionAmount must be positive")
            @Digits(integer = 13, fraction = 2) BigDecimal contributionAmount,
            @Min(value = 2, message = "a circle needs at least 2 seats")
            @Max(value = 60, message = "seatCount may not exceed 60") int seatCount,
            @Min(value = 1, message = "cycleLengthDays must be positive")
            @Max(value = 366) int cycleLengthDays,
            @NotNull Boolean shariaMode,
            /** Optional; falls back to the platform default fee schedule when omitted. */
            UUID feeScheduleId,
            /** The creator's own slot preference; defaults to FLEXIBLE if omitted. */
            SlotPreference ownerSlotPreference,
            /** Private circle — joining requires an invite code. Defaults to false. */
            Boolean inviteOnly
    ) {
    }

    public record JoinCircleRequest(
            @NotNull SlotPreference slotPreference,
            /** Required when the circle is invite-only. */
            String inviteCode
    ) {
    }

    public record MemberView(
            UUID userId,
            SlotPreference slotPreference,
            Integer slotPosition,
            MemberStanding standing
    ) {
        static MemberView from(CircleMember m) {
            return new MemberView(m.getUserId(), m.getSlotPreference(), m.getSlotPosition(), m.getStanding());
        }
    }

    public record CircleView(
            UUID id,
            String name,
            BigDecimal contributionAmount,
            int seatCount,
            int filledSeats,
            int cycleLengthDays,
            CircleStatus status,
            boolean shariaMode,
            boolean inviteOnly,
            UUID ownerId,
            Instant createdAt
    ) {
        static CircleView of(Circle c, int filledSeats) {
            return new CircleView(c.getId(), c.getName(), c.getContributionAmount(), c.getSeatCount(),
                    filledSeats, c.getCycleLengthDays(), c.getStatus(), c.isShariaMode(),
                    c.isInviteOnly(), c.getOwnerId(), c.getCreatedAt());
        }
    }

    public record CircleDetailView(
            CircleView circle,
            List<MemberView> members
    ) {
    }
}
