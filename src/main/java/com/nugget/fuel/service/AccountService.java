package com.nugget.fuel.service;

import com.nugget.fuel.dto.RegisterRequest;
import com.nugget.fuel.model.AppUser;
import com.nugget.fuel.model.Role;
import com.nugget.fuel.model.Station;
import com.nugget.fuel.repo.StationRepository;
import com.nugget.fuel.repo.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Account sign-up. Everyone who registers here becomes a fuel ATTENDANT; nobody can pick another role. */
@Service
@Transactional
public class AccountService {
    private final UserRepository users;
    private final StationRepository stations;
    private final PasswordEncoder encoder;

    public AccountService(UserRepository users, StationRepository stations, PasswordEncoder encoder) {
        this.users = users; this.stations = stations; this.encoder = encoder;
    }

    private ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }

    /** Branch list for the sign-up form (id and name only). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> branches() {
        return stations.findAll().stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName())).toList();
    }

    private String cleanName(String raw) {
        String n = raw.trim().replaceAll("\\s+", " ");
        if (!n.matches("\\p{L}[\\p{L} '\\-]{0,39}")) throw bad("Names can only contain letters (up to 40 characters).");
        return n;
    }

    public Map<String, String> register(RegisterRequest req) {
        String phone = req.phone().replaceAll("[\\s\\-()]", "");
        if (!phone.matches("\\+?[0-9]{7,15}")) {
            throw bad("Enter a valid phone number, digits only (for example 0771234567).");
        }
        if (users.findByUsername(phone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This phone number is already registered. Please sign in.");
        }
        Station branch = stations.findById(req.branchId()).orElseThrow(() -> bad("Please choose your branch."));
        String full = cleanName(req.firstName()) + " " + cleanName(req.surname());
        // keep names unique so two people with the same name never get merged in the totals
        if (users.existsByFullName(full)) full = full + " (" + phone.substring(phone.length() - 4) + ")";
        users.save(new AppUser(phone, full, encoder.encode(req.password()), Role.ATTENDANT, branch));
        return Map.of("username", phone, "name", full);
    }
}
