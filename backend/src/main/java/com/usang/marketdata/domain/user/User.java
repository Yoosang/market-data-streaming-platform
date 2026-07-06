package com.usang.marketdata.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")  // "user"는 MySQL 예약어이므로 "users" 사용
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static User of(String username, String passwordHash) {
        User u = new User();
        u.username = username;
        u.passwordHash = passwordHash;
        u.createdAt = LocalDateTime.now();
        return u;
    }
}
