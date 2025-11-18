// com/example/demo/entity/TalentPost.java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="talent_posts",
        indexes = {
                @Index(name = "idx_tp_status_created", columnList = "status, createdAt"),
                @Index(name = "idx_tp_category", columnList = "category"),
                @Index(name = "idx_tp_author", columnList = "author_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentPost extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=80)
    private String title;

    @Column(nullable=false, length=2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private TalentCategory category;

    // ✅ 다대다: 게시글 ↔ 태그
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "talent_post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            uniqueConstraints = @UniqueConstraint(
                    name="uk_tp_tag_pair",
                    columnNames = {"post_id","tag_id"}
            )
    )
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    @Column(nullable=false, length=1000)
    private String extraNote;

    // ✅ 대표 이미지(첫 번째 이미지) – 기존 프론트/DB와 호환용
    @Column(length=300)
    private String portfolioImageUrl;

    // ✅ 여러 장 이미지 URL 리스트
    @ElementCollection
    @CollectionTable(
            name = "talent_post_images",
            joinColumns = @JoinColumn(name = "post_id")
    )
    @Column(name = "image_url", length = 300, nullable = false)
    @OrderColumn(name = "ord")
    @Builder.Default
    private List<String> portfolioImageUrls = new ArrayList<>();

    @Column(nullable=false)
    private Integer price;

    @Column(length=100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private TalentStatus status;

    @Column(nullable=false)
    private Long views;

    @Column(nullable=false)
    private Long likesCount;

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    private User author;

    public void increaseViews(){ this.views = this.views + 1; }
}
