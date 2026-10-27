package com.nugget.fuel.web;

import com.nugget.fuel.dto.Dtos.*;
import com.nugget.fuel.dto.PriceInfo;
import com.nugget.fuel.dto.PriceRequest;
import com.nugget.fuel.dto.ReadingResponse;
import com.nugget.fuel.service.ReadingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** Everything under /api/manager is for managers only. */
@RestController
@RequestMapping("/api/manager")
public class ManagerController {
    private final ReadingService service;
    public ManagerController(ReadingService service) { this.service = service; }

    /** Branches, with the currencies each one accepts. */
    @GetMapping("/stations")
    public List<StationInfo> stations() { return service.stationInfos(); }

    @GetMapping("/readings")
    public List<ReadingResponse> readings(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.forManager(date, stationId);
    }

    /** The daily total page for one branch (cash by currency, expenses, credit, litres). */
    @GetMapping("/summary")
    public StationSummary summary(@RequestParam Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.stationSummary(stationId, date);
    }

    @GetMapping("/prices")
    public List<PriceInfo> prices() { return service.prices(); }

    @PostMapping("/prices")
    public List<PriceInfo> setPrice(@Valid @RequestBody PriceRequest req, Principal p) {
        return service.setPrice(p.getName(), req);
    }

    @GetMapping("/currencies")
    public List<CurrencyInfo> currencies() { return service.currencyList(); }

    /** Add a currency or change its exchange rate. */
    @PostMapping("/currencies")
    public List<CurrencyInfo> saveCurrency(@Valid @RequestBody CurrencyRequest req, Principal p) {
        return service.saveCurrency(p.getName(), req);
    }

    /** Choose which currencies a branch accepts. */
    @PostMapping("/stations/{id}/currencies")
    public List<StationInfo> stationCurrencies(@PathVariable Long id, @Valid @RequestBody StationCurrenciesRequest req) {
        return service.setStationCurrencies(id, req);
    }

    /** Discount per litre for each fuel type (and any scheduled change). */
    @GetMapping("/discounts")
    public List<DiscountInfo> discounts() { return service.discounts(); }

    /** Manager sets the discount per litre, effective today or from a later date. */
    @PostMapping("/discounts")
    public List<DiscountInfo> setDiscount(@Valid @RequestBody DiscountRequest req, Principal p) {
        return service.setDiscount(p.getName(), req);
    }
}
