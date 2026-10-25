package com.nugget.fuel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Small request/response shapes for currencies, cash and the daily branch summary. */
public final class Dtos {
    private Dtos() {}

    // ----- currencies -----
    public record CurrencyInfo(String code, String name, BigDecimal unitsPerUsd) {}
    public record CurrencyRequest(@NotBlank String code, @NotBlank String name,
                                  @NotNull @Positive BigDecimal unitsPerUsd) {}
    public record StationInfo(Long id, String name, List<String> currencies) {}
    public record StationCurrenciesRequest(@NotNull List<String> currencies) {}

    // ----- attendant cash handed in -----
    public record CashLineRequest(@NotBlank String currency, @NotNull @PositiveOrZero BigDecimal amount) {}
    public record CashRequest(@NotNull List<@Valid CashLineRequest> lines) {}
    public record CashLine(String currency, BigDecimal amount, BigDecimal rate, BigDecimal usdValue) {}
    public record CashSheet(List<CashLine> lines, List<CurrencyInfo> accepted,
                            BigDecimal declaredUsd, BigDecimal expectedUsd) {}

    // ----- supervisor: stock and the read-only branch pages -----
    /** First day: send opening + closing. Later days: send deliveries + returns. The server decides which applies. */
    public record StockRequest(@NotBlank String fuelType, LocalDate date,
            @PositiveOrZero BigDecimal opening, @PositiveOrZero BigDecimal closing,
            @PositiveOrZero BigDecimal deliveries, @PositiveOrZero BigDecimal returns) {}
    /**
     * One fuel type's stock for a day.
     * needsSetup = no records yet (first day), beforeStart = date is before the first day,
     * baselineDay = the first day itself. Otherwise opening = previous closing and
     * closing = opening + deliveries + returns - salesLitres.
     */
    public record StockLine(String fuelType, boolean needsSetup, boolean beforeStart, boolean baselineDay,
            LocalDate baselineDate, BigDecimal opening, BigDecimal deliveries, BigDecimal returns,
            BigDecimal salesLitres, BigDecimal closing) {}
    public record AttendantPage(String attendant, List<ReadingResponse> readings, List<CashLine> cash,
            BigDecimal expectedUsd, BigDecimal declaredUsd) {}

    // ----- manager's daily branch summary -----
    public record CurrencyTotal(String code, String name, BigDecimal amount, BigDecimal rate, BigDecimal usdValue) {}
    public record CreditLine(String name, BigDecimal amount) {}
    public record FuelLitres(String fuelType, BigDecimal litres) {}
    public record AttendantCash(String attendant, BigDecimal expectedUsd, BigDecimal declaredUsd) {}
    public record StationSummary(
            Long stationId, String station, LocalDate date, int pumpsReported,
            BigDecimal salesUsd, BigDecimal expectedCashUsd,
            List<CurrencyTotal> cash, BigDecimal declaredUsd,
            BigDecimal companyFuel, BigDecimal lunch, BigDecimal transport, BigDecimal expensesTotal,
            BigDecimal creditTotal, List<CreditLine> credits,
            List<FuelLitres> litres, List<AttendantCash> attendants,
            List<StockLine> stock) {}
}
