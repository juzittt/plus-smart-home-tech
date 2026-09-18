package ru.yandex.practicum.gateway.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private List<User> users;

    @Getter
    @Setter
    public static class User {
        private String username;
        private String password;
        private List<String> roles;
    }
}