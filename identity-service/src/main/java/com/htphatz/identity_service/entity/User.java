package com.htphatz.identity_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    private String id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;
}
