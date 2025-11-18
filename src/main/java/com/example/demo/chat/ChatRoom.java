// com/example/demo/chat/ChatRoom.java
package com.example.demo.chat;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="chat_room",
        uniqueConstraints=@UniqueConstraint(name="uk_room_post_owner_other",
                columnNames={"postId","ownerId","otherUserId"}))
public class ChatRoom {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long postId;      // 재능글 ID
    private Long ownerId;     // 글 작성자(User.id)
    private Long otherUserId; // 채팅을 건 사람(User.id)
}
