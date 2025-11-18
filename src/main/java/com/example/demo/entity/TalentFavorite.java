package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "talent_favorites",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "post_id"})
        }
)
public class TalentFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 즐겨찾기한 유저 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** 즐겨찾기된 재능 글 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private TalentPost post;

    protected TalentFavorite() {
    }

    public TalentFavorite(User user, TalentPost post) {
        this.user = user;
        this.post = post;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public TalentPost getPost() {
        return post;
    }
}
