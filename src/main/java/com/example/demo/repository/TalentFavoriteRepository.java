package com.example.demo.repository;

import com.example.demo.entity.TalentFavorite;
import com.example.demo.entity.TalentPost;
import com.example.demo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TalentFavoriteRepository extends JpaRepository<TalentFavorite, Long> {

    boolean existsByUserAndPost(User user, TalentPost post);

    Optional<TalentFavorite> findByUserAndPost(User user, TalentPost post);

    long countByPost(TalentPost post);

    /** ⭐ 특정 유저의 즐겨찾기 목록 (페이지네이션) */
    Page<TalentFavorite> findByUser(User user, Pageable pageable);
}
