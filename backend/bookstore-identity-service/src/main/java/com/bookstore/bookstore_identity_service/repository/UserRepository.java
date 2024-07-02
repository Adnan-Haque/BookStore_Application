package com.bookstore.bookstore_identity_service.repository;

import com.bookstore.bookstore_identity_service.config.CustomUserDetails;
import com.bookstore.bookstore_identity_service.entity.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface UserRepository extends ReactiveMongoRepository<User, Integer> {
    Mono<User> findByUsername(String username);
}
