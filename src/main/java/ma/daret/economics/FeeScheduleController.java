package ma.daret.economics;

import jakarta.validation.Valid;
import ma.daret.economics.FeeScheduleDtos.CreateFeeScheduleRequest;
import ma.daret.economics.FeeScheduleDtos.FeeScheduleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/fee-schedules")
@PreAuthorize("hasRole('ADMIN')")
public class FeeScheduleController {

    private final FeeScheduleService service;

    public FeeScheduleController(FeeScheduleService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeeScheduleResponse create(@Valid @RequestBody CreateFeeScheduleRequest req) {
        return FeeScheduleResponse.from(service.create(req));
    }

    @GetMapping
    public List<FeeScheduleResponse> list() {
        return service.list().stream().map(FeeScheduleResponse::from).toList();
    }

    @GetMapping("/{id}")
    public FeeScheduleResponse get(@PathVariable UUID id) {
        return FeeScheduleResponse.from(service.get(id));
    }
}
