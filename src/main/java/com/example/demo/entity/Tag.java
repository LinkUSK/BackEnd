package com.example.demo.entity;

import jakarta.persistence.*;
        import lombok.*;

@Entity
@Table(name="tags",
        uniqueConstraints = @UniqueConstraint(name="uk_tag_category_name", columnNames = {"category","name"}),
        indexes = {
                @Index(name="idx_tag_category", columnList="category"),
                @Index(name="idx_tag_name", columnList="name")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private TalentCategory category;

    @Column(nullable=false, length=30)
    private String name;

    @Column(nullable=false)
    private boolean active;
}
