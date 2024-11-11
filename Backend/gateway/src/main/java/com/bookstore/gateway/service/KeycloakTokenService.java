package com.bookstore.gateway.service;

import com.bookstore.gateway.dto.CredentialsDTO;
import com.bookstore.gateway.dto.TokenResponseDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

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
    private String realAdminPassword;

    private final WebClient webClient;

    public KeycloakTokenService(WebClient.Builder webClientBuilder) {
        this.webClient = WebClient.builder().build();
    }

    private String tokenLink;

    private String userLink;

    @PostConstruct
    public void init(){
        tokenLink = "http://host.docker.internal:8080/realms/"+realmName+"/protocol/openid-connect/token";
        userLink = "http://host.docker.internal:8080/admin/realms/"+realmName+"/users";

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
                        .with("password", realAdminPassword))
                .retrieve().bodyToMono(TokenResponseDTO.class)
                .map(TokenResponseDTO::getAccess_token);
    }

    public Mono<String> createUser(CredentialsDTO credentialsDTO){
        return webClient.post()
                .uri(userLink)
                .header("Authorization", "Bearer" + getAdminToken())
                .bodyValue(credentialsDTO)
                .retrieve().bodyToMono(String.class);
    }

}
