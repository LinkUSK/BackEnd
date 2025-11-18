package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User extends BaseTime {   // 🔹 BaseTime 상속 (createdAt, updatedAt 사용)

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 20)
    private String userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;     // 학교 이메일

    @Column(nullable = false, length = 100)
    private String password;  // 해시 저장

    // ▼ 추가
    @Column(length = 100)
    private String major; // 전공

    @Column(length = 255)
    private String profileImageUrl; // 프로필 이미지 URL (/files/xxx)

    // ✅ 가입일은 BaseTime의 createdAt 필드를 그대로 사용 (별도 필드 필요 X)
}
