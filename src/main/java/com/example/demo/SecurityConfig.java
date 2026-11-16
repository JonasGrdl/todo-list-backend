package com.example.demo;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import java.time.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
class SecurityConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean Argon2PasswordEncoder passwords() { return new Argon2PasswordEncoder(16, 32, 1, 19456, 2); }
    @Bean HttpSessionSecurityContextRepository contexts() { return new HttpSessionSecurityContextRepository(); }

    @Bean SecurityFilterChain security(HttpSecurity http, Clock clock, HttpSessionSecurityContextRepository contexts,
                                       @Value("${app.frontend}") String frontend, Environment env) throws Exception {
        URI origin = URI.create(frontend);
        boolean localHttp = env.acceptsProfiles(Profiles.of("dev")) && "http".equals(origin.getScheme());
        if ((!localHttp && !"https".equals(origin.getScheme())) || origin.getHost() == null
                || origin.getUserInfo() != null || !origin.getRawPath().isEmpty()
                || origin.getQuery() != null || origin.getFragment() != null) {
            throw new IllegalArgumentException("FRONTEND_URL muss eine einzelne HTTPS-Origin ohne Pfad sein (HTTP im dev-Profil).");
        }
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(frontend));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return http.cors(c -> c.configurationSource(source))
                .securityContext(c -> c.securityContextRepository(contexts))
                .requestCache(c -> c.disable())
                .addFilterBefore(new SessionLifetime(clock), SecurityContextHolderFilter.class)
                .authorizeHttpRequests(c -> c.requestMatchers("/auth/profile").hasRole("VERIFIED")
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/todos", "/todos/**").hasRole("VERIFIED").anyRequest().denyAll())
                .exceptionHandling(c -> c.authenticationEntryPoint((req, res, ex) -> res.setStatus(401)))
                .logout(c -> c.logoutUrl("/auth/logout").invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID").logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
                .build();
    }

    static class SessionLifetime extends OncePerRequestFilter {
        private final Clock clock;
        SessionLifetime(Clock clock) { this.clock = clock; }

        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                  FilterChain chain) throws IOException, ServletException {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("loginAt") instanceof Instant login) {
                Instant now = clock.instant();
                Instant last = (Instant) session.getAttribute("activeAt");
                if (!now.isBefore(login.plus(Duration.ofHours(24))) || !now.isBefore(last.plus(Duration.ofHours(2)))) {
                    session.invalidate();
                } else {
                    session.setAttribute("activeAt", now);
                }
            }
            chain.doFilter(request, response);
        }
    }
}
