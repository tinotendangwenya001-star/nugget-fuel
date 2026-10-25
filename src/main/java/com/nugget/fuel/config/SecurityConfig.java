package com.nugget.fuel.config;

import com.nugget.fuel.repo.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/index.html", "/error").permitAll()       // the web page (and error messages)
                .requestMatchers("/api/public/**").permitAll()                   // create-an-account page
                .requestMatchers("/api/manager/**").hasRole("MANAGER")
                .requestMatchers("/api/supervisor/**").hasRole("SUPERVISOR")     // view-only pages + stock
                .requestMatchers("/api/readings/**").hasRole("ATTENDANT")        // only attendants enter readings
                .anyRequest().authenticated())
            // plain 401 (no browser popup) so the page can show its own login error
            .httpBasic(b -> b.authenticationEntryPoint((req, res, ex) -> res.setStatus(401)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername()).password(u.getPassword())
                        .roles(u.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
