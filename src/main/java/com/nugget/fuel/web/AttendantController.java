package com.nugget.fuel.web;

import com.nugget.fuel.dto.*;
import com.nugget.fuel.dto.Dtos.CashRequest;
import com.nugget.fuel.dto.Dtos.CashSheet;
import com.nugget.fuel.service.ReadingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AttendantController {
    private final ReadingService service;
    public AttendantController(ReadingService service) { this.service = service; }

    @GetMapping("/today")
    public Map<String, String> today() { return Map.of("today", service.today().toString()); }

    @GetMapping("/me")
    public Map<String, String> me(Principal p) { return service.me(p.getName()); }

    @GetMapping("/pumps")
    public List<PumpDto> pumps(Principal p) { return service.pumpsFor(p.getName()); }

    @GetMapping("/readings/context")
    public PumpContext context(@RequestParam Long pumpId, Principal p) {
        return service.context(p.getName(), pumpId);
    }

    @PostMapping("/readings")
    public ReadingResponse submit(@Valid @RequestBody ReadingRequest req, Principal p) {
        return service.submit(p.getName(), req);
    }

    @PostMapping("/readings/fills")
    public ReadingResponse addFill(@Valid @RequestBody FillRequest req, Principal p) {
        return service.addFill(p.getName(), req);
    }

    @DeleteMapping("/readings/fills/{id}")
    public ReadingResponse deleteFill(@PathVariable Long id, Principal p) {
        return service.deleteFill(p.getName(), id);
    }

    /** Cash the attendant has handed in today, by currency, and the currencies their branch accepts. */
    @GetMapping("/readings/cash")
    public CashSheet cash(Principal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.cashSheet(p.getName(), date);
    }

    @PostMapping("/readings/cash")
    public CashSheet saveCash(@Valid @RequestBody CashRequest req, Principal p) {
        return service.saveCash(p.getName(), req);
    }

    @GetMapping("/readings/mine")
    public List<ReadingResponse> mine(Principal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.mine(p.getName(), date);
    }
}
