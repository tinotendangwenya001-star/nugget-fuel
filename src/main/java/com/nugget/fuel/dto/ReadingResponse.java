package com.nugget.fuel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReadingResponse(
        Long readingId,
        String station,
        String pump,
        String attendant,
        LocalDate date,
        boolean firstTimeSetup,
        LocalDate openingDate,
        BigDecimal pricePerLitre,
        Values opening,
        Values closing,
        Values difference,       // closing - opening
        List<FillDto> fills,     // company cars and creditors
        BigDecimal creditTotal,  // value of all fills
        BigDecimal cashTotal,    // sales difference - creditTotal
        BigDecimal lunchAmount,
        BigDecimal transportAmount) {

    public record Values(BigDecimal manual, BigDecimal volume, BigDecimal sales) {}

    public record FillDto(Long id, String type, String companyName, String driverName, String vehicleReg,
                          BigDecimal litres, BigDecimal amount) {}
}
