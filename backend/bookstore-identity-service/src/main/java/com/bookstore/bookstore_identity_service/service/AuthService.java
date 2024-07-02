package com.bookstore.bookstore_identity_service.service;

import com.bookstore.bookstore_identity_service.entity.User;
import com.bookstore.bookstore_identity_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Mono<String> saveUser(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user).then(Mono.just("User Added Successfully"));
    }

    public Mono<String> generateToken(String username){
        return Mono.just(jwtService.generateToken(username));
    }

    public boolean validateToken(String token){
        return jwtService.validateToken(token);
    }
}
