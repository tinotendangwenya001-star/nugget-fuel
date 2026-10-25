package com.nugget.fuel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Price per litre for a fuel type, in force from effectiveDate until a later price is set. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"fuelType", "effectiveDate"}))
public class FuelPrice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fuelType;          // DIESEL / PETROL
    private LocalDate effectiveDate;
    @Column(precision = 18, scale = 4) private BigDecimal pricePerLitre;
    private String setBy;

    protected FuelPrice() {}
    public FuelPrice(String fuelType, LocalDate effectiveDate) {
        this.fuelType = fuelType; this.effectiveDate = effectiveDate;
    }
    public void set(BigDecimal price, String setBy) { this.pricePerLitre = price; this.setBy = setBy; }

    public String getFuelType() { return fuelType; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public BigDecimal getPricePerLitre() { return pricePerLitre; }
}
