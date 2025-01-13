package com.htphatz.identity_service.repository;

import com.htphatz.identity_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
}
