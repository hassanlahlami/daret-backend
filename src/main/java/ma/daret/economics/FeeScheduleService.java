package ma.daret.economics;

import ma.daret.common.web.ApiException;
import ma.daret.economics.FeeScheduleDtos.CreateFeeScheduleRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FeeScheduleService {

    private final FeeScheduleRepository repository;
    private final FeeEngine feeEngine;

    public FeeScheduleService(FeeScheduleRepository repository, FeeEngine feeEngine) {
        this.repository = repository;
        this.feeEngine = feeEngine;
    }

    @Transactional
    public FeeSchedule create(CreateFeeScheduleRequest req) {
        repository.findByName(req.name()).ifPresent(s -> {
            throw ApiException.conflict("FEE_SCHEDULE_NAME_TAKEN", "A fee schedule with this name exists");
        });

        FeeSchedule schedule = new FeeSchedule();
        schedule.setName(req.name().trim());
        schedule.setServiceFee(req.serviceFee());
        schedule.setBaseFee(req.baseFee());
        schedule.setRedistributionShare(req.redistributionShare());
        schedule.setTierMultipliers(req.tierMultipliers());

        // Reject schedules that would let payout bonuses exceed position fees.
        feeEngine.assertMarginInvariant(schedule);

        return repository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<FeeSchedule> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public FeeSchedule get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("FEE_SCHEDULE_NOT_FOUND", "Fee schedule not found"));
    }
}
