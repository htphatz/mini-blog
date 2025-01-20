package com.htphatz.profile_service.repository;

import com.htphatz.profile_service.entity.Profile;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends Neo4jRepository<Profile, String> {
    Optional<Profile> findByUserId(String userId);
}
