package com.nugget.fuel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    private String fullName;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    /** Attendants belong to one station; managers have null (they see all). */
    @ManyToOne
    private Station station;

    protected AppUser() {}
    public AppUser(String username, String fullName, String password, Role role, Station station) {
        this.username = username; this.fullName = fullName; this.password = password;
        this.role = role; this.station = station;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public Station getStation() { return station; }
}
