// src/main/java/com/example/demo/chat/LinkuReviewRepository.java
package com.example.demo.chat;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LinkuReviewRepository extends JpaRepository<LinkuReview, Long> {

    // 특정 LinkU 연결에 대해, 내가 이미 리뷰를 남겼는지
    boolean existsByConnection_IdAndReviewer(Long connectionId, User reviewer);

    // 내가 '대상(target)'인 리뷰 목록 (내가 받은 후기들)
    List<LinkuReview> findByTargetOrderByCreatedAtDesc(User target);

    // ⭐ 내/상대 별점 평균 구하기 (kindnessScore 평균)
    @Query("select avg(r.kindnessScore) from LinkuReview r where r.target.id = :targetId")
    Double findAverageKindnessScoreByTargetId(@Param("targetId") Long targetId);

    // ⭐ 내/상대가 받은 리뷰 개수
    long countByTarget_Id(Long targetId);

    // ✅ 특정 LinkU 연결에 대한 최신 리뷰
    Optional<LinkuReview> findFirstByConnection_IdOrderByCreatedAtDesc(Long connectionId);
}
