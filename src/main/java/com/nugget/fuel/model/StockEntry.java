package com.nugget.fuel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tank stock records for one fuel type at one branch (litres), kept by the supervisor.
 * The FIRST record (baseline = true) holds the opening and closing stock typed in on the first day.
 * Every later day only stores deliveries and returns; the opening and closing are worked out
 * (opening = previous closing, closing = opening + deliveries + returns - litres sold).
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"station_id", "fuelType", "stockDate"}))
public class StockEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Station station;
    private String fuelType;     // DIESEL / PETROL
    private LocalDate stockDate;
    private boolean baseline;    // true = the first day
    @Column(precision = 18, scale = 2) private BigDecimal openingStock;    // baseline only
    @Column(precision = 18, scale = 2) private BigDecimal closingStock;    // baseline only
    @Column(precision = 18, scale = 2) private BigDecimal deliveries;      // later days
    @Column(precision = 18, scale = 2) private BigDecimal returnedLitres;  // later days: fuel returned to the tank
    private String enteredBy;

    protected StockEntry() {}
    public StockEntry(Station station, String fuelType, LocalDate date, boolean baseline) {
        this.station = station; this.fuelType = fuelType; this.stockDate = date; this.baseline = baseline;
    }
    public void setBaseline(BigDecimal opening, BigDecimal closing, String by) {
        this.openingStock = opening; this.closingStock = closing; this.enteredBy = by;
    }
    public void setMovements(BigDecimal deliveries, BigDecimal returned, String by) {
        this.deliveries = deliveries; this.returnedLitres = returned; this.enteredBy = by;
    }

    public LocalDate getStockDate() { return stockDate; }
    public BigDecimal getOpeningStock() { return openingStock; }
    public BigDecimal getClosingStock() { return closingStock; }
    public BigDecimal getDeliveries() { return deliveries; }
    public BigDecimal getReturns() { return returnedLitres; }
}
