package com.nugget.fuel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Cash an attendant hands in for a day, in one currency. The rate is kept as it was when saved. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"attendant_id", "declDate", "currencyCode"}))
public class CashDeclaration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private AppUser attendant;
    private LocalDate declDate;
    private String currencyCode;
    @Column(precision = 18, scale = 2) private BigDecimal amount;
    @Column(precision = 18, scale = 6) private BigDecimal rate;   // units per US$1 at save time

    protected CashDeclaration() {}
    public CashDeclaration(AppUser attendant, LocalDate date, String code, BigDecimal amount, BigDecimal rate) {
        this.attendant = attendant; this.declDate = date; this.currencyCode = code;
        this.amount = amount; this.rate = rate;
    }

    public AppUser getAttendant() { return attendant; }
    public LocalDate getDeclDate() { return declDate; }
    public String getCurrencyCode() { return currencyCode; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getRate() { return rate; }
}
