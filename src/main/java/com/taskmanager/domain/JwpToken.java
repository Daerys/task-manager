package com.taskmanager.domain;

import com.taskmanager.domain.enums.TokenType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "token", unique = true, nullable = false)
    private String token;

    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    @Column(name = "is_revoked")
    private boolean isRevoked;

    @Column(name = "is_expired")
    private boolean isExpired;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "expires_at")
    private Date expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public JwpToken(String token, TokenType tokenType, Date createdAt, Date expiresAt, User user) {
        this.token = token;
        this.tokenType = tokenType;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.user = user;
        this.isRevoked = false;
        this.isExpired = false;
    }
}
