// src/main/java/com/example/demo/chat/LinkuReview.java
package com.example.demo.chat;

import com.example.demo.entity.BaseTime;
import com.example.demo.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "linku_reviews")
@Getter
@Setter
@NoArgsConstructor
public class LinkuReview extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 LinkU 연결에 대한 후기인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false)
    private LinkuConnection connection;

    // 리뷰를 남기는 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    // 리뷰를 받는 사람 (마이페이지에서 보는 대상 = 나)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_rating", nullable = false, length = 20)
    private RelationRating relationRating;   // BAD / GOOD / BEST

    @Column(nullable = false)
    private int kindnessScore;

    @Column(nullable = false, length = 1000)
    private String content;

    /** 관계 평점 enum */
    public enum RelationRating {
        BAD,
        GOOD,
        BEST
    }

    public LinkuReview(
            LinkuConnection connection,
            User reviewer,
            User target,
            RelationRating relationRating,
            int kindnessScore,
            String content
    ) {
        this.connection = connection;
        this.reviewer = reviewer;
        this.target = target;
        this.relationRating = relationRating;
        this.kindnessScore = kindnessScore;
        this.content = content;
    }
}
