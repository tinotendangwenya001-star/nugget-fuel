package com.nugget.fuel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A currency the company handles. unitsPerUsd = how many units equal US$1 (USD itself is 1). */
@Entity
public class Currency {
    @Id private String code;
    private String name;
    @Column(precision = 18, scale = 6) private BigDecimal unitsPerUsd;
    private String updatedBy;

    protected Currency() {}
    public Currency(String code, String name, BigDecimal unitsPerUsd) {
        this.code = code; this.name = name; this.unitsPerUsd = unitsPerUsd;
    }
    public void set(String name, BigDecimal unitsPerUsd, String by) {
        this.name = name; this.unitsPerUsd = unitsPerUsd; this.updatedBy = by;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getUnitsPerUsd() { return unitsPerUsd; }
}
