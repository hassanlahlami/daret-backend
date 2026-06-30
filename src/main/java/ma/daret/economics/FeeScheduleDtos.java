package ma.daret.economics;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Admin payloads for managing fee schedules (spec §6 ADMIN). */
public final class FeeScheduleDtos {

    private FeeScheduleDtos() {
    }

    public record CreateFeeScheduleRequest(
            @NotBlank @Size(max = 80) String name,
            @NotNull @DecimalMin("0.0") BigDecimal serviceFee,
            @NotNull @DecimalMin("0.0") BigDecimal baseFee,
            @NotNull @DecimalMin("0.0") BigDecimal redistributionShare,
            /** Tier name (A–E) → position-fee multiplier. */
            @NotNull Map<String, BigDecimal> tierMultipliers
    ) {
    }

    public record FeeScheduleResponse(
            UUID id,
            String name,
            BigDecimal serviceFee,
            BigDecimal baseFee,
            BigDecimal redistributionShare,
            Map<String, BigDecimal> tierMultipliers
    ) {
        public static FeeScheduleResponse from(FeeSchedule s) {
            return new FeeScheduleResponse(s.getId(), s.getName(), s.getServiceFee(),
                    s.getBaseFee(), s.getRedistributionShare(), s.getTierMultipliers());
        }
    }
}
