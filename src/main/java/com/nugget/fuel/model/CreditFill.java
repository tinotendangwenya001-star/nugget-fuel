package com.nugget.fuel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Fuel given to a company car or a creditor (no cash taken). Value = litres x that day's price. */
@Entity
public class CreditFill {
    public enum Type { COMPANY, CREDITOR }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private FuelReading reading;
    @Enumerated(EnumType.STRING) private Type type;
    private String companyName;   // creditor / company name (required for creditors)
    private String driverName;
    private String vehicleReg;
    @Column(precision = 18, scale = 2) private BigDecimal litres;

    protected CreditFill() {}
    public CreditFill(FuelReading reading, Type type, String companyName, String driverName,
                      String vehicleReg, BigDecimal litres) {
        this.reading = reading; this.type = type; this.companyName = companyName;
        this.driverName = driverName; this.vehicleReg = vehicleReg; this.litres = litres;
    }

    public Long getId() { return id; }
    public FuelReading getReading() { return reading; }
    public Type getType() { return type; }
    public String getCompanyName() { return companyName; }
    public String getDriverName() { return driverName; }
    public String getVehicleReg() { return vehicleReg; }
    public BigDecimal getLitres() { return litres; }
}
