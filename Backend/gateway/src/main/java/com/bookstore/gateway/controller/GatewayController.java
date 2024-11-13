package com.bookstore.gateway.controller;

import com.bookstore.gateway.dto.Credentials;
import com.bookstore.gateway.dto.CredentialsDTO;
import com.bookstore.gateway.dto.TokenResponseDTO;
import com.bookstore.gateway.service.KeycloakTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
public class GatewayController {

    private final KeycloakTokenService keycloakTokenService;

    public GatewayController(KeycloakTokenService keycloakTokenService) {
        this.keycloakTokenService = keycloakTokenService;
    }

    @GetMapping("/health")
    public Mono<String> healthCheck(){
        return Mono.just("OK");
    }

    @PostMapping("/login-user")
    public Mono<ResponseEntity<Object>> login(@RequestBody Map<String, String> map, ServerWebExchange exchange) {
        return keycloakTokenService.getToken(map)
                .flatMap(tokenResponse -> {
                    ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", tokenResponse.getAccess_token())
                            .httpOnly(true)
                            .path("/")
                            .maxAge(tokenResponse.getExpires_in()) // Set based on token life
                            .build();

                    // Create refresh token cookie
                    ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", tokenResponse.getRefresh_token())
                            .httpOnly(true)
                            .path("/")
                            .maxAge(tokenResponse.getRefresh_expires_in()) // Set longer for refresh token
                            .build();

                    // Add cookies to the response
                    exchange.getResponse().addCookie(accessTokenCookie);
                    exchange.getResponse().addCookie(refreshTokenCookie);

                    return Mono.just(ResponseEntity.ok((Object) tokenResponse));
                })
                .onErrorResume(ResponseStatusException.class, ex -> {
                    return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(ex.getReason()));
                });
    }

    @PostMapping("/register-user")
    public Mono<ResponseEntity<String>> register(@RequestBody CredentialsDTO credentialsDTO) {
        return keycloakTokenService.createUser(credentialsDTO)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body("User Created"))
                .onErrorResume(ResponseStatusException.class, ex -> {
                    return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(ex.getReason()));
                });
    }

    @PostMapping("/logout-user")
    public Mono<ResponseEntity<String>> logout(@RequestBody String refreshToken) {
        return keycloakTokenService.logout(refreshToken)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> Mono.just(ResponseEntity.status(500).body("Logout failed: " + error.getMessage())));
    }
}
