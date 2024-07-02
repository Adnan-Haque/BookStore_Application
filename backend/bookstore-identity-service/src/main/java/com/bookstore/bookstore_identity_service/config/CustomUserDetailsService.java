package com.bookstore.bookstore_identity_service.config;

import com.bookstore.bookstore_identity_service.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository repository) {
        this.userRepository = repository;
    }


    @Override
    public Mono<UserDetails> findByUsername(String username) {
        // Create UserDetails object from your User entity
        return userRepository.findByUsername(username)
                .map(CustomUserDetails::new).cast(UserDetails.class)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)));
    }
}
