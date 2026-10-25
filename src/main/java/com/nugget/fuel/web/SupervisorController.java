package com.nugget.fuel.web;

import com.nugget.fuel.dto.Dtos.AttendantPage;
import com.nugget.fuel.dto.Dtos.StationSummary;
import com.nugget.fuel.dto.Dtos.StockLine;
import com.nugget.fuel.dto.Dtos.StockRequest;
import com.nugget.fuel.service.ReadingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/**
 * Supervisors only. The branch always comes from the supervisor's own account (never from the request),
 * so a supervisor can only ever see and change their own branch. Nothing here edits an attendant's page.
 */
@RestController
@RequestMapping("/api/supervisor")
public class SupervisorController {
    private final ReadingService service;
    public SupervisorController(ReadingService service) { this.service = service; }

    /** Every attendant's page at the supervisor's branch (read only). */
    @GetMapping("/pages")
    public List<AttendantPage> pages(Principal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.supervisorPages(p.getName(), date);
    }

    /** The same daily summary the manager sees, for the supervisor's own branch. */
    @GetMapping("/summary")
    public StationSummary summary(Principal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.supervisorSummary(p.getName(), date);
    }

    @GetMapping("/stock")
    public List<StockLine> stock(Principal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.supervisorStock(p.getName(), date);
    }

    @PostMapping("/stock")
    public List<StockLine> saveStock(@Valid @RequestBody StockRequest req, Principal p) {
        return service.saveStock(p.getName(), req);
    }
}
