package ru.yandex.practicum.gateway.security;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GET /api/products без учётных данных → 200 OK")
    void catalogGet_isPublic() {
        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /api/categories без учётных данных → 200 OK")
    void categoriesGet_isPublic() {
        webTestClient.get().uri("/api/categories")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /api/inventory/{id} без учётных данных → 200 OK")
    void inventoryGet_isPublic() {
        webTestClient.get().uri("/api/inventory/1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("OPTIONS /api/orders (preflight) → проходит без учётных данных")
    void corsPreflight_isPublic() {
        webTestClient.method(HttpMethod.OPTIONS).uri("/api/orders")
                .exchange()
                .expectStatus().value(status -> Assertions.assertThat(status)
                        .isNotIn(401, 403));
    }

    @Test
    @DisplayName("POST /api/orders без учётных данных → 401 Unauthorized")
    void orderCreate_withoutCredentials_isUnauthorized() {
        webTestClient.post().uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/orders с неверным паролем → 401 Unauthorized")
    void orderCreate_withWrongPassword_isUnauthorized() {
        webTestClient.post().uri("/api/orders")
                .header("Authorization", basic("ivan", "wrong-password"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/orders с ivan:ivan → проходит security (200)")
    void orderCreate_withUserCredentials_passesSecurity() {
        webTestClient.post().uri("/api/orders")
                .header("Authorization", basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /api/orders/by-email с ivan:ivan → проходит security (200)")
    void ordersByEmail_withUserCredentials_passesSecurity() {
        webTestClient.get().uri("/api/orders/by-email?email=ivan@example.com")
                .header("Authorization", basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /api/orders/{id} с ivan:ivan → проходит security (200)")
    void orderById_withUserCredentials_passesSecurity() {
        webTestClient.get().uri("/api/orders/1")
                .header("Authorization", basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /api/orders (список) с ivan:ivan → 403 Forbidden")
    void allOrders_withUserCredentials_isForbidden() {
        webTestClient.get().uri("/api/orders")
                .header("Authorization", basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /api/products с ivan:ivan → 403 Forbidden")
    void productWrite_withUserCredentials_isForbidden() {
        webTestClient.post().uri("/api/products")
                .header("Authorization", basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /api/products с anna:anna → проходит security (200)")
    void productWrite_withAdminCredentials_passesSecurity() {
        webTestClient.post().uri("/api/products")
                .header("Authorization", basic("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("PUT /api/products/1 с anna:anna → проходит security (200)")
    void productUpdate_withAdminCredentials_passesSecurity() {
        webTestClient.put().uri("/api/products/1")
                .header("Authorization", basic("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("PATCH /api/products/1 с ivan:ivan → 403 Forbidden")
    void productPatch_withUserCredentials_isForbidden() {
        webTestClient.patch().uri("/api/products/1")
                .header("Authorization", basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("DELETE /api/products/1 с ivan:ivan → 403 Forbidden")
    void productDelete_withUserCredentials_isForbidden() {
        webTestClient.delete().uri("/api/products/1")
                .header("Authorization", basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("GET /api/orders (список) с anna:anna → проходит security (200)")
    void allOrders_withAdminCredentials_passesSecurity() {
        webTestClient.get().uri("/api/orders")
                .header("Authorization", basic("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /api/inventory с anna:anna → проходит security (200)")
    void inventoryWrite_withAdminCredentials_passesSecurity() {
        webTestClient.post().uri("/api/inventory")
                .header("Authorization", basic("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /api/inventory с ivan:ivan → 403 Forbidden")
    void inventoryWrite_withUserCredentials_isForbidden() {
        webTestClient.post().uri("/api/inventory")
                .header("Authorization", basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Неизвестный маршрут с anna:anna → 403 Forbidden")
    void unknownRoute_withAdminCredentials_isForbidden() {
        webTestClient.get().uri("/api/unknown-route")
                .header("Authorization", basic("anna", "anna"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Неизвестный маршрут без учётных данных → 401 Unauthorized")
    void unknownRoute_withoutCredentials_isUnauthorized() {
        webTestClient.get().uri("/api/unknown-route")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String basic(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route(GET("/api/products"), req -> ServerResponse.ok().build())
                    .andRoute(GET("/api/products/{id}"), req -> ServerResponse.ok().build())
                    .andRoute(POST("/api/products"), req -> ServerResponse.ok().build())
                    .andRoute(PUT("/api/products/{id}"), req -> ServerResponse.ok().build())
                    .andRoute(PATCH("/api/products/{id}"), req -> ServerResponse.ok().build())
                    .andRoute(DELETE("/api/products/{id}"), req -> ServerResponse.ok().build())
                    .andRoute(GET("/api/categories"), req -> ServerResponse.ok().build())
                    .andRoute(GET("/api/categories/{id}"), req -> ServerResponse.ok().build())
                    .andRoute(POST("/api/categories"), req -> ServerResponse.ok().build())
                    .andRoute(GET("/api/inventory"), req -> ServerResponse.ok().build())
                    .andRoute(GET("/api/inventory/{id}"), req -> ServerResponse.ok().build())
                    .andRoute(POST("/api/inventory"), req -> ServerResponse.ok().build())
                    .andRoute(PUT("/api/inventory"), req -> ServerResponse.ok().build())
                    .andRoute(GET("/api/orders"), req -> ServerResponse.ok().build())
                    .andRoute(GET("/api/orders/{id}"), req -> ServerResponse.ok().build())
                    .andRoute(GET("/api/orders/by-email"), req -> ServerResponse.ok().build())
                    .andRoute(POST("/api/orders"), req -> ServerResponse.ok().build());
        }
    }
}