package ma.daret.admin;

import ma.daret.circles.CircleStatus;
import ma.daret.identity.kyc.KycStatus;
import ma.daret.identity.user.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read models for the ADMIN back-office console. */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record Overview(
            Map<String, Long> circlesByStatus,
            long totalUsers,
            BigDecimal platformRevenue,
            BigDecimal platformExposure,
            long gapFilledSlots
    ) {
    }

    public record AdminCircleView(
            UUID id,
            String name,
            CircleStatus status,
            int seatCount,
            int filledSeats,
            BigDecimal contributionAmount,
            boolean shariaMode,
            UUID ownerId,
            Instant createdAt
    ) {
    }

    public record AdminUserView(
            UUID id,
            String email,
            String phone,
            Set<Role> roles,
            boolean enabled,
            KycStatus kycStatus
    ) {
    }

    public record SetEnabledRequest(boolean enabled) {
    }

    public record WeeklyPoint(String label, long count) {
    }

    public record Analytics(
            Map<String, Long> contributions,
            Map<String, Long> payouts,
            long openDisputes,
            long openAmlFlags,
            long totalVouches,
            java.util.List<WeeklyPoint> newCirclesPerWeek
    ) {
    }
}
