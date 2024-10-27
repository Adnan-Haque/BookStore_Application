package com.bookstore.gateway.controller;

import com.bookstore.gateway.dto.TokenResponse;
import com.bookstore.gateway.dto.UserRequestDTO;
import com.bookstore.gateway.service.KeycloakTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    public final KeycloakTokenService keycloakTokenService;

    public AuthenticationController(KeycloakTokenService keycloakTokenService) {
        this.keycloakTokenService = keycloakTokenService;
    }

    @GetMapping("/health")
    public Mono<String> health(){
        logger.info("health happend");
        return Mono.just("OK");
    }

    @PostMapping("/login-user")
    public Mono<TokenResponse> generateToken(@RequestBody Map<String, String> map){
        if(map.containsKey("username") && map.containsKey("password")){
            logger.info("Loggin in.....");
            return keycloakTokenService.getUserToken(map.get("username"), map.get("password"));
        }
        return null;
    }

    @PostMapping("/register-user")
    public Mono<String> generateToken(@RequestBody UserRequestDTO userRequestDto){
        boolean containsPassword = userRequestDto.getCredentials().stream().anyMatch(cred -> "password".equals(cred.getType())
                && cred.getValue() != null && !cred.getValue().isEmpty());

        if(!containsPassword) return Mono.just("Password not Present");

//        logger.info(keycloakTokenService.getAccessToken().toString());
        return keycloakTokenService.userCreation(userRequestDto);
    }

    @PostMapping("/logout-user")
    public Mono<String> logoutUser(@RequestBody Map<String,String> payload){
        if(!payload.containsKey("refresh_token")) return Mono.just("Refresh Token Empty");
        String refreshToken = payload.get("refresh_token");
        logger.info("logging out the user....");
        keycloakTokenService.logoutUser(refreshToken);
        return Mono.just("Logout Successfull");
    }

}
