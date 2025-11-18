// src/main/java/com/example/demo/chat/ChatRoomRepository.java
package com.example.demo.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 내가 owner 또는 other 인 방들 (채팅 목록용)
    List<ChatRoom> findByOwnerIdOrOtherUserId(Long ownerId, Long otherUserId);

    // 예전 로직 (postId까지 포함해서 찾던 것) - 다른 곳에서 쓸 수도 있으니 남겨두되,
    // getOrCreateRoom에서는 더 이상 사용하지 않을 거야.
    Optional<ChatRoom> findByPostIdAndOwnerIdAndOtherUserId(Long postId, Long ownerId, Long otherUserId);

    // 🔹 유저 쌍으로 방 찾기 (순서 고려)
    Optional<ChatRoom> findByOwnerIdAndOtherUserId(Long ownerId, Long otherUserId);

    // 🔹 유저 쌍으로 방 찾기 (A,B) 혹은 (B,A) 둘 다 검색
    Optional<ChatRoom> findByOwnerIdAndOtherUserIdOrOwnerIdAndOtherUserId(
            Long ownerId1, Long otherUserId1,
            Long ownerId2, Long otherUserId2
    );
}
