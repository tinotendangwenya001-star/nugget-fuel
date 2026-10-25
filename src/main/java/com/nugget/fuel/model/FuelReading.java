package com.nugget.fuel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One reading per pump per day: opening (previous closing), closing, and that day's fuel price. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pump_id", "readingDate"}))
public class FuelReading {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Pump pump;
    @ManyToOne(optional = false) private AppUser attendant;
    private LocalDate readingDate;
    /** Date the opening values came from. Equals readingDate for a first-time setup entry. */
    private LocalDate openingDate;
    @Column(precision = 18, scale = 2) private BigDecimal openingManual;
    @Column(precision = 18, scale = 2) private BigDecimal openingVolume;
    @Column(precision = 18, scale = 2) private BigDecimal openingSales;
    @Column(precision = 18, scale = 2) private BigDecimal closingManual;
    @Column(precision = 18, scale = 2) private BigDecimal closingVolume;
    @Column(precision = 18, scale = 2) private BigDecimal closingSales;
    /** Only on the lunch pump (Diesel Pump B): attendant's lunch, and transport money on Mondays. */
    @Column(precision = 18, scale = 2) private BigDecimal lunchAmount;
    @Column(precision = 18, scale = 2) private BigDecimal transportAmount;

    protected FuelReading() {}
    public FuelReading(Pump pump, AppUser attendant, LocalDate date) {
        this.pump = pump; this.attendant = attendant; this.readingDate = date;
    }

    public void set(AppUser attendant, LocalDate openingDate,
                    BigDecimal om, BigDecimal ov, BigDecimal os,
                    BigDecimal cm, BigDecimal cv, BigDecimal cs) {
        this.attendant = attendant; this.openingDate = openingDate;
        this.openingManual = om; this.openingVolume = ov; this.openingSales = os;
        this.closingManual = cm; this.closingVolume = cv; this.closingSales = cs;
    }

    public void setExpenses(BigDecimal lunch, BigDecimal transport) { this.lunchAmount = lunch; this.transportAmount = transport; }
    public BigDecimal getLunchAmount() { return lunchAmount; }
    public BigDecimal getTransportAmount() { return transportAmount; }

    public Long getId() { return id; }
    public Pump getPump() { return pump; }
    public AppUser getAttendant() { return attendant; }
    public LocalDate getReadingDate() { return readingDate; }
    public LocalDate getOpeningDate() { return openingDate; }
    public BigDecimal getOpeningManual() { return openingManual; }
    public BigDecimal getOpeningVolume() { return openingVolume; }
    public BigDecimal getOpeningSales() { return openingSales; }
    public BigDecimal getClosingManual() { return closingManual; }
    public BigDecimal getClosingVolume() { return closingVolume; }
    public BigDecimal getClosingSales() { return closingSales; }
}
