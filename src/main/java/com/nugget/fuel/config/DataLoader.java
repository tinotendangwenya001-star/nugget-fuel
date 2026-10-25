package com.nugget.fuel.config;

import com.nugget.fuel.model.*;
import com.nugget.fuel.repo.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates the starting data ONCE: branches, pumps, currencies, fuel prices and the manager account.
 * If the database already has users (for example the online PostgreSQL database after a restart),
 * nothing is created again and all existing data is kept.
 */
@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner seed(StationRepository stations, PumpRepository pumps, UserRepository users,
                           FuelPriceRepository priceRepo, CurrencyRepository currencies, PasswordEncoder enc,
                           @Value("${nugget.demo-users:true}") boolean demoUsers,
                           @Value("${nugget.demo-password:password}") String demoPassword,
                           @Value("${nugget.manager-password:password}") String managerPassword) {
        return args -> {
            if (users.count() > 0) return;   // already set up: keep the existing data

            currencies.save(new Currency("USD", "US Dollar", BigDecimal.ONE));
            currencies.save(new Currency("ZAR", "RSA (South African Rand)", new BigDecimal("18.00")));

            for (String type : new String[]{"DIESEL", "PETROL"}) {
                FuelPrice fp = new FuelPrice(type, LocalDate.now().minusDays(30));
                fp.set(new BigDecimal("2.00"), "system");
                priceRepo.save(fp);
            }

            String[] names = {"Nugget Khami", "Nugget Kelvin", "Nugget Gwanda", "Nugget Lupane"};  // demo branches
            Station[] st = new Station[names.length];
            for (int i = 0; i < names.length; i++) {
                Station s = new Station(names[i]);
                s.setCurrencies(List.of("USD", "ZAR"));
                st[i] = stations.save(s);
                pumps.save(new Pump("Diesel Pump A", "DIESEL", st[i]));
                pumps.save(new Pump("Diesel Pump B", "DIESEL", st[i], true));   // lunch / Monday transport
                pumps.save(new Pump("Petrol Pump A", "PETROL", st[i]));
                pumps.save(new Pump("Petrol Pump B", "PETROL", st[i]));
            }

            if (demoUsers) {
                String pw = enc.encode(demoPassword);
                users.save(new AppUser("alice", "Alice (Khami)", pw, Role.ATTENDANT, st[0]));
                users.save(new AppUser("carol", "Carol (Khami)", pw, Role.ATTENDANT, st[0]));
                users.save(new AppUser("bob", "Bob (Kelvin)", pw, Role.ATTENDANT, st[1]));
                users.save(new AppUser("dan", "Dan (Gwanda)", pw, Role.ATTENDANT, st[2]));
                users.save(new AppUser("eve", "Eve (Lupane)", pw, Role.ATTENDANT, st[3]));
                users.save(new AppUser("sam", "Sam (Khami Supervisor)", pw, Role.SUPERVISOR, st[0]));
                users.save(new AppUser("sue", "Sue (Kelvin Supervisor)", pw, Role.SUPERVISOR, st[1]));
            }
            users.save(new AppUser("manager", "Head Manager", enc.encode(managerPassword), Role.MANAGER, null));
        };
    }
}
