package com.bookstore.gateway.service;

import com.bookstore.gateway.dto.TokenResponse;
import com.bookstore.gateway.dto.UserRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class KeycloakTokenService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakTokenService.class);

    private final WebClient webClient;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.uri}")
    private String baseUri;

    private final String tokenLink = baseUri+"realms/"+realm+"/protocol/openid-connect/token";

    public KeycloakTokenService(WebClient.Builder webClientBuilder) {
        this.webClient = WebClient.builder().build();
    }

    public Mono<TokenResponse> getUserToken(String username, String password) {
        return webClient.post()
                .uri(tokenLink)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("username", username)
                        .with("password", password))
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }

    public Mono<String> getAccessToken() {
        return webClient.post()
                .uri(tokenLink)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", "client_credentials"))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(TokenResponse::getAccess_token)
                .doOnNext(token -> logger.info("Access Token: " + token));  // Log the token for debugging
    }

    public Mono<String> userCreation(UserRequestDTO userRequestDTO){
        return getAccessToken().flatMap(accessToken -> {
            return webClient.post()
                    .uri("http://host.docker.internal:8080/admin/realms/BookStoreApplication/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(userRequestDTO))  // Directly passing UserRequest
                    .retrieve()
                    .bodyToMono(String.class)
                    .then(Mono.just("User Successfull Created"))
                    .onErrorResume(e -> Mono.just("Error creating user: " + e.getMessage()));
        });
    }

    public void logoutUser(String refreshToken) {
        webClient.post()
            .uri("http://host.docker.internal:8080/realms/BookStoreApplication/protocol/openid-connect/logout")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters
                    .fromFormData("client_id", clientId)
                    .with("client_secret", clientSecret)
                    .with("refresh_token", refreshToken))
            .retrieve()
            .bodyToMono(String.class);
    }
}
