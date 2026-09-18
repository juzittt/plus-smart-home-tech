package ru.yandex.practicum.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final SecurityProperties securityProperties;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {})
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .httpBasic(httpBasic -> {})

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/inventory/**").permitAll()
                        .pathMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()

                        .pathMatchers(HttpMethod.POST, "/api/orders/**").hasRole("USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders/by-email").hasRole("USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders/{id}").hasRole("USER")

                        .pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")

                        .pathMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        .pathMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/categories/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        .pathMatchers(HttpMethod.POST, "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/inventory/**").hasRole("ADMIN")

                        .anyExchange().denyAll()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService(PasswordEncoder encoder) {
        List<SecurityProperties.User> configuredUsers = securityProperties.getUsers();

        if (configuredUsers == null || configuredUsers.isEmpty()) {
            log.warn("No users configured via app.security.users, using fallback users");
            configuredUsers = List.of(
                    createUser("ivan", "{noop}ivan", List.of("USER")),
                    createUser("anna", "{noop}anna", List.of("USER", "ADMIN"))
            );
        }

        var users = configuredUsers.stream()
                .map(u -> User.builder()
                        .username(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRoles().toArray(new String[0]))
                        .build())
                .toList();

        log.info("Loaded {} users: {}", users.size(),
                users.stream().map(UserDetails::getUsername).toList());

        return new MapReactiveUserDetailsService(users);
    }

    private SecurityProperties.User createUser(String username, String password, List<String> roles) {
        SecurityProperties.User user = new SecurityProperties.User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRoles(roles);
        return user;
    }
}