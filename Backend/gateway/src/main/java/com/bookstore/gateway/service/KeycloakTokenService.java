package com.bookstore.gateway.service;

import com.bookstore.gateway.dto.CredentialsDTO;
import com.bookstore.gateway.dto.TokenResponseDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class KeycloakTokenService {

    @Value("${REALM_NAME}")
    private String realmName;

    @Value("${KEYCLOAK_CLIENT_ID}")
    private String clientId;

    @Value("${KEYCLOAK_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${KEYCLOAK_REALM_ADMIN_PASSWORD}")
    private String realmAdminPassword;

    private final WebClient webClient;

    public KeycloakTokenService(WebClient.Builder webClientBuilder) {
        this.webClient = WebClient.builder().build();
    }

    private String tokenLink;

    private String userLink;

    private String logoutLink;

    @PostConstruct
    public void init(){
        tokenLink = "https://host.docker.internal:8443/realms/"+realmName+"/protocol/openid-connect/token";
        userLink = "https://host.docker.internal:8443/admin/realms/"+realmName+"/users";
        logoutLink = "https://host.docker.internal:8443/realms/"+realmName+"/protocol/openid-connect/logout";
    }

    public Mono<TokenResponseDTO> getToken(Map<String, String> map){
        if(!map.containsKey("username") && !map.containsKey("password")){
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User Credentials not present"));
        }

        String username = map.get("username");
        String password = map.get("password");

        if(username == null || password == null || username.isBlank() || password.isBlank()){
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User Credentials not present"));
        }

        return webClient.post()
                .uri(tokenLink)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", "password")
                        .with("username", username)
                        .with("password",password))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    if(clientResponse.statusCode() == HttpStatus.UNAUTHORIZED){
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,"User Credentials Invalid"));
                    }
                    return Mono.error( new ResponseStatusException(clientResponse.statusCode(), "Internal Server Error"));
                })
                .bodyToMono(TokenResponseDTO.class);
    }

    private Mono<String> getAdminToken(){
        return webClient.post()
                .uri(tokenLink)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", "password")
                        .with("username", "realmadmin")
                        .with("password", realmAdminPassword))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    return Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Error Processing admin Credentials"));
                }).bodyToMono(TokenResponseDTO.class)
                .map(TokenResponseDTO::getAccess_token);
    }

    public Mono<String> createUser(CredentialsDTO credentialsDTO, String roleName){
        return getAdminToken()
                .flatMap(adminToken ->
                        webClient.post()
                                .uri(userLink) // replace with the actual URI for user creation
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .bodyValue(credentialsDTO)
                                .retrieve()
                                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                                    if(clientResponse.statusCode() == HttpStatus.CONFLICT){
                                        return Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Username Already Exists"));
                                    }
                                    return Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Error Creating user"));
                                })
                                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                                    return Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Internal Server Error"));
                                })
                                .bodyToMono(String.class)
                                .then(findUserId(adminToken, credentialsDTO.getUsername()))
                                .flatMap(userId -> assignRoleToUserWithRetryAndRollback(adminToken, userId, roleName))
                );
    }

    private Mono<String> assignRoleToUserWithRetryAndRollback(String adminToken, String userId, String roleName) {
        return assignRole(adminToken, userId, roleName)
                .retry(3) // Retry up to 3 times on failure
                .onErrorResume(ex -> deleteUser(adminToken, userId) // Rollback if retries fail
                        .then(Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to assign role; user creation rolled back", ex))));
    }

    private Mono<Void> deleteUser(String adminToken, String userId) {
        return webClient.delete()
                .uri("http://host.docker.internal:8080/admin/realms/{realm}/users/{userId}", realmName, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Error deleting user during rollback"))
                )
                .bodyToMono(Void.class);
    }

    private Mono<String> findUserId(String adminToken, String username) {
        return webClient.get()
                .uri("http://host.docker.internal:8080/admin/realms/{realm}/users?username={username}", realmName, username)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Error finding user ID"))
                )
                .bodyToFlux(Map.class)
                .filter(user -> username.equals(user.get("username")))
                .next() // Take the first matching user
                .map(user -> (String) user.get("id")); // Extract the userId
    }

    // Helper method to assign role to user
    private Mono<String> assignRole(String adminToken, String userId, String roleName) {
        return getRoleId(adminToken, roleName)
                .flatMap(roleId ->
                        webClient.post()
                                .uri("http://host.docker.internal:8080/admin/realms/{realm}/users/{userId}/role-mappings/realm", realmName, userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .bodyValue(List.of(Map.of("id", roleId, "name", roleName))) // Assign role by ID and name
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, clientResponse ->
                                        Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Error assigning role to user"))
                                )
                                .bodyToMono(String.class)
                );
    }

    // Helper method to get Role ID
    private Mono<String> getRoleId(String adminToken, String roleName) {
        return webClient.get()
                .uri("http://host.docker.internal:8080/admin/realms/{realm}/roles/{roleName}", realmName, roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Error retrieving role ID"))
                )
                .bodyToMono(Map.class)
                .map(roleMap -> (String) roleMap.get("id")); // Extract the role ID
    }

    public Mono<String> logout(String refreshToken) {
        System.out.println(refreshToken);

        return webClient.post()
                .uri(logoutLink)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", refreshToken))
                .retrieve()
                .toBodilessEntity()
                .flatMap(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return Mono.just("User logged out successfully");
                    } else {
                        return Mono.error(new RuntimeException("Logout failed with status: " + response.getStatusCode()));
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    // Log the error (optional)
                    System.err.println("Logout error: " + ex.getMessage());
                    // Map the error response
                    return Mono.just("Logout failed: " + ex.getStatusText());
                });
    }

}
