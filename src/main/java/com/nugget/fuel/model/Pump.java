package com.nugget.fuel.model;

import jakarta.persistence.*;

@Entity
public class Pump {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;       // e.g. "Diesel Pump B"
    private String fuelType;   // DIESEL / PETROL
    @ManyToOne(optional = false)
    private Station station;
    /** The attendant on this pump may enter lunch (and, on Mondays, transport) money. */
    private boolean lunchPump;

    protected Pump() {}
    public Pump(String name, String fuelType, Station station) { this(name, fuelType, station, false); }
    public Pump(String name, String fuelType, Station station, boolean lunchPump) {
        this.name = name; this.fuelType = fuelType; this.station = station; this.lunchPump = lunchPump;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getFuelType() { return fuelType; }
    public Station getStation() { return station; }
    public boolean isLunchPump() { return lunchPump; }
}
