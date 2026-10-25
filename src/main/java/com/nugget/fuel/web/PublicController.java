package com.nugget.fuel.web;

import com.nugget.fuel.dto.RegisterRequest;
import com.nugget.fuel.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Open to everyone (no sign-in needed): used by the "Create an account" page. */
@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final AccountService accounts;
    public PublicController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/branches")
    public List<Map<String, Object>> branches() { return accounts.branches(); }

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody RegisterRequest req) { return accounts.register(req); }
}
