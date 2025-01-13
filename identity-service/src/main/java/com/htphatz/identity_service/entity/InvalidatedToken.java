package com.htphatz.identity_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "invalidated_tokens")
@Entity
public class InvalidatedToken {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "expiration_time")
    private Date expirationTime;
}
