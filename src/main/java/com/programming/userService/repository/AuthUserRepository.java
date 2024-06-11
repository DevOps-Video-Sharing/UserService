package com.programming.userService.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.programming.userService.entity.AuthUser;

import java.util.Optional;

@Repository
public interface AuthUserRepository extends MongoRepository<AuthUser, String> {
    Optional<AuthUser> findByUsername(String username);
}