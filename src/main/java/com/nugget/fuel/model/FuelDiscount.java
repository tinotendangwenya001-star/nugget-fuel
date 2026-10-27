package com.nugget.fuel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Discount per litre (dollars taken off each litre) for a fuel type, set by the manager. Stays in force until changed. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"fuelType", "effectiveDate"}))
public class FuelDiscount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fuelType;          // DIESEL / PETROL
    private LocalDate effectiveDate;
    @Column(precision = 18, scale = 4) private BigDecimal discountPerLitre;
    private String setBy;

    protected FuelDiscount() {}
    public FuelDiscount(String fuelType, LocalDate effectiveDate) {
        this.fuelType = fuelType; this.effectiveDate = effectiveDate;
    }
    public void set(BigDecimal discount, String setBy) { this.discountPerLitre = discount; this.setBy = setBy; }

    public String getFuelType() { return fuelType; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public BigDecimal getDiscountPerLitre() { return discountPerLitre; }
}
