// src/main/java/com/example/demo/chat/LinkuConnection.java
package com.example.demo.chat;

import com.example.demo.entity.BaseTime;
import com.example.demo.entity.User;
import com.example.demo.entity.TalentPost;   // 🔹 추가
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "linku_connections")
@Getter
@Setter
@NoArgsConstructor
public class LinkuConnection extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 채팅방에서 이루어진 LinkU 인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    // 🔹 어떤 재능글 기준으로 맺어진 LinkU 인지 (새로 추가)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "talent_post_id")
    private TalentPost talentPost;

    // 제안한 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // 제안을 받은 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LinkuStatus status = LinkuStatus.PENDING;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public enum LinkuStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}
