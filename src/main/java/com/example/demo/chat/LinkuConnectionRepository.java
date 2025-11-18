// src/main/java/com/example/demo/chat/LinkuConnectionRepository.java
package com.example.demo.chat;

import com.example.demo.chat.LinkuConnection.LinkuStatus;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LinkuConnectionRepository extends JpaRepository<LinkuConnection, Long> {

    // room(연관필드)의 id 기준으로 검색할 때는 room_Id 로 작성해야 함!
    Optional<LinkuConnection> findFirstByRoom_IdAndStatusOrderByCreatedAtDesc(
            Long roomId,
            LinkuStatus status
    );

    Optional<LinkuConnection> findFirstByRoom_IdAndRequesterAndTargetOrderByCreatedAtDesc(
            Long roomId,
            User requester,
            User target
    );

    /**
     * 진행 중인 협업 수
     * - ACCEPTED 상태이고 completed = false 인 것만 카운트
     */
    @Query("""
            select count(c)
            from LinkuConnection c
            where c.status = com.example.demo.chat.LinkuConnection.LinkuStatus.ACCEPTED
              and c.completed = false
              and (c.requester.id = :userId or c.target.id = :userId)
            """)
    long countOngoingByUserId(@Param("userId") Long userId);

    /**
     * 진행한 협업 수
     * - ACCEPTED 상태인 모든 LinkU (completed 여부 상관 X)
     */
    @Query("""
            select count(c)
            from LinkuConnection c
            where c.status = com.example.demo.chat.LinkuConnection.LinkuStatus.ACCEPTED
              and (c.requester.id = :userId or c.target.id = :userId)
            """)
    long countAcceptedByUserId(@Param("userId") Long userId);

    /**
     * ✅ 내 링크유 목록
     * - ACCEPTED 이고 completed = true 인 것만 (협업 종료 + 리뷰 작성 완료)
     * - 내가 requester 이거나 target 인 것
     */
    @Query("""
        select c
        from LinkuConnection c
        join fetch c.requester
        join fetch c.target
        join fetch c.room
        left join fetch c.talentPost   
        where c.status = com.example.demo.chat.LinkuConnection.LinkuStatus.ACCEPTED
          and c.completed = true
          and (c.requester.id = :userId or c.target.id = :userId)
        order by c.createdAt desc
        """)
    List<LinkuConnection> findCompletedByUserId(@Param("userId") Long userId);
}
