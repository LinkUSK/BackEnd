// src/main/java/com/example/demo/chat/ChatRoomExit.java
package com.example.demo.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "chat_room_exit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomExit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 방에서
    @Column(nullable = false)
    private Long roomId;

    // 누가 나갔는지 (User.id)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant exitedAt;
}
