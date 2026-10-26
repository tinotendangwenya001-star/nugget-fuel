package com.nugget.fuel.service;

import com.nugget.fuel.dto.RegisterRequest;
import com.nugget.fuel.model.AppUser;
import com.nugget.fuel.model.Role;
import com.nugget.fuel.model.Station;
import com.nugget.fuel.repo.StationRepository;
import com.nugget.fuel.repo.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Account sign-up for attendants, supervisors and managers.
 *  - One phone number = one account = one role, so a person who registered as an attendant or supervisor
 *    cannot register again as a manager.
 *  - Supervisor and manager sign-up needs a company access code (nugget.supervisor-code / nugget.manager-code).
 *    If a code is empty, sign-up for that role is switched off.
 */
@Service
@Transactional
public class AccountService {
    private final UserRepository users;
    private final StationRepository stations;
    private final PasswordEncoder encoder;
    private final String supervisorCode;
    private final String managerCode;
    private final ConcurrentLinkedDeque<Long> wrongCodes = new ConcurrentLinkedDeque<>();

    public AccountService(UserRepository users, StationRepository stations, PasswordEncoder encoder,
                          @Value("${nugget.supervisor-code:}") String supervisorCode,
                          @Value("${nugget.manager-code:}") String managerCode) {
        this.users = users; this.stations = stations; this.encoder = encoder;
        this.supervisorCode = supervisorCode; this.managerCode = managerCode;
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

    private Role parseRole(String raw) {
        if (raw == null || raw.isBlank()) return Role.ATTENDANT;
        try { return Role.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw bad("Unknown role."); }
    }

    /** Supervisors and managers must know the company's access code. Too many wrong guesses pauses sign-up. */
    private void checkCode(Role role, String given) {
        String expected = role == Role.MANAGER ? managerCode : supervisorCode;
        String label = role.name().toLowerCase();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Registering as a " + label + " is switched off. Please ask the company for access.");
        }
        long now = System.currentTimeMillis();
        while (!wrongCodes.isEmpty() && now - wrongCodes.peekFirst() > 15 * 60 * 1000L) wrongCodes.pollFirst();
        if (wrongCodes.size() >= 10) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong codes. Please try again in 15 minutes.");
        }
        boolean ok = given != null && MessageDigest.isEqual(
                given.trim().getBytes(StandardCharsets.UTF_8), expected.trim().getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            wrongCodes.add(now);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That access code is not correct.");
        }
    }

    public Map<String, String> register(RegisterRequest req) {
        Role role = parseRole(req.role());
        if (role != Role.ATTENDANT) checkCode(role, req.accessCode());

        String phone = req.phone().replaceAll("[\\s\\-()]", "");
        if (!phone.matches("\\+?[0-9]{7,15}")) {
            throw bad("Enter a valid phone number, digits only (for example 0771234567).");
        }
        // one person, one account: the phone number can only be used once, whatever the role
        if (users.findByUsername(phone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This phone number already has an account. One person can only have one account, so please sign in.");
        }

        Station branch = null;
        if (role != Role.MANAGER) {   // managers see every branch
            if (req.branchId() == null) throw bad("Please choose your branch.");
            branch = stations.findById(req.branchId()).orElseThrow(() -> bad("Please choose your branch."));
        }

        String full = cleanName(req.firstName()) + " " + cleanName(req.surname());
        // keep names unique so two people with the same name never get merged in the totals
        if (users.existsByFullName(full)) full = full + " (" + phone.substring(phone.length() - 4) + ")";
        users.save(new AppUser(phone, full, encoder.encode(req.password()), role, branch));
        return Map.of("username", phone, "name", full, "role", role.name());
    }
}
