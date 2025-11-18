// src/main/java/com/example/demo/chat/ChatRoomExitRepository.java
package com.example.demo.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomExitRepository extends JpaRepository<ChatRoomExit, Long> {

    // 방 + 유저 조합으로 "나간 기록이 있나" 확인
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    // 마지막 나가기 기록 (가장 최근 exitedAt 기준)
    Optional<ChatRoomExit> findTopByRoomIdAndUserIdOrderByExitedAtDesc(Long roomId, Long userId);
}
