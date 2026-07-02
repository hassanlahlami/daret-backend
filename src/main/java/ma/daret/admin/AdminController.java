package ma.daret.admin;

import jakarta.validation.Valid;
import ma.daret.admin.AdminDtos.AdminCircleView;
import ma.daret.admin.AdminDtos.AdminUserView;
import ma.daret.admin.AdminDtos.Overview;
import ma.daret.admin.AdminDtos.SetEnabledRequest;
import ma.daret.circles.CircleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** ADMIN back-office: platform overview, circle & user administration. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public Overview overview() {
        return adminService.overview();
    }

    @GetMapping("/circles")
    public Page<AdminCircleView> circles(@RequestParam(required = false) CircleStatus status,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return adminService.circles(status, pageable);
    }

    @GetMapping("/users")
    public Page<AdminUserView> users(@PageableDefault(size = 20) Pageable pageable) {
        return adminService.users(pageable);
    }

    @PostMapping("/users/{id}/enabled")
    public AdminUserView setEnabled(@PathVariable UUID id, @Valid @RequestBody SetEnabledRequest req) {
        return adminService.setEnabled(id, req.enabled());
    }
}
