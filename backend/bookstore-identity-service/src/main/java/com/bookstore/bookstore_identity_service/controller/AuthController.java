package com.bookstore.bookstore_identity_service.controller;

import com.bookstore.bookstore_identity_service.dto.AuthRequest;
import com.bookstore.bookstore_identity_service.entity.User;
import com.bookstore.bookstore_identity_service.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {


    private final AuthService authService;

    private final ReactiveAuthenticationManager authenticationManager;

    public AuthController(AuthService authService, ReactiveAuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }


    @PostMapping("/register")
    public Mono<String> addNewUser(@RequestBody User user){
        return authService.saveUser(user);
    }


    @PostMapping("/login")
    public Mono<String> generateToken(@RequestBody AuthRequest authRequest) {
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword());

        return authenticationManager.authenticate(authenticationToken).then(
                authService.generateToken(authRequest.getUsername())
                )
                .onErrorResume(ex -> Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication Failed: "+ ex.getMessage())));
    }

    @GetMapping("/validate")
    public Mono<String> login(@RequestParam String token){
        return Mono.fromCallable(() -> {
            if (authService.validateToken(token)) {
                return "Token is Valid";
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token");
            }
        }).onErrorResume(ex -> {
            if (ex instanceof ResponseStatusException) {
                return Mono.error(ex);
            } else {
                return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
            }
        });
    }
}
