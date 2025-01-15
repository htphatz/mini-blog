package com.htphatz.profile_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Node("profile")
public class Profile {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    @Property(value = "firstName")
    private String firstName;

    @Property(value = "lastName")
    private String lastName;

    @Property(value = "phoneNumber")
    private String phoneNumber;

    @Property(value = "address")
    private String address;

    @Property(value = "userId")
    private String userId;
}
