package com.nugget.fuel.model;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** A branch of the company (Khami, Kelvin, Gwanda, Lupane ...). */
@Entity
public class Station {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    /** Currency codes this branch accepts (set by the manager). USD is always included. */
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> currencies = new TreeSet<>();

    protected Station() {}
    public Station(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Set<String> getCurrencies() { return currencies; }
    public void setCurrencies(Collection<String> codes) { currencies.clear(); currencies.addAll(codes); }
}
