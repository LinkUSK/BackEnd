// src/main/java/com/example/demo/chat/ChatMessageRepository.java
package com.example.demo.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 가장 최근 메시지 50개 (내림차순)
    List<ChatMessage> findTop50ByRoomIdOrderByCreatedAtDesc(Long roomId);

    // 마지막 메시지 1개 (방 목록에서 사용)
    Optional<ChatMessage> findTop1ByRoomIdOrderByCreatedAtDesc(Long roomId);

    // 특정 시각 이후의 최근 50개 메시지 (예전용 – 안 쓰게 될 예정)
    List<ChatMessage> findTop50ByRoomIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long roomId,
            Instant createdAt
    );

    // 특정 시각 이후에 메시지가 하나라도 있는지 여부 (방 숨김 여부 판단용)
    boolean existsByRoomIdAndCreatedAtAfter(Long roomId, Instant createdAt);

    // ✅ 이 방의 전체 메시지 (오래된 순)
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);

    // ✅ 나간 시각 이후의 전체 메시지 (오래된 순)
    List<ChatMessage> findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long roomId,
            Instant createdAt
    );

    // ===================== 🔹 unread 계산용 =====================

    // 방 + 수신자 기준으로 아직 읽지 않은 메시지 개수
    long countByRoomIdAndReceiverIdAndReadFlagFalse(Long roomId, Long receiverId);

    // 방 + 수신자 + 특정 시각 이후의 아직 읽지 않은 메시지 개수
    long countByRoomIdAndReceiverIdAndCreatedAtAfterAndReadFlagFalse(
            Long roomId,
            Long receiverId,
            Instant createdAt
    );

    // ===================== 🔹 읽음 처리용 =====================

    @Modifying
    @Query("""
           update ChatMessage m
              set m.readFlag = true
            where m.roomId = :roomId
              and m.receiverId = :userId
              and m.readFlag = false
           """)
    int markAsReadInRoom(@Param("roomId") Long roomId,
                         @Param("userId") Long userId);
}
