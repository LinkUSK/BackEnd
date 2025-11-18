// src/main/java/com/example/demo/repository/TalentPostSpecs.java
package com.example.demo.repository;

import com.example.demo.entity.TalentCategory;
import com.example.demo.entity.TalentPost;
import com.example.demo.entity.TalentStatus;
import com.example.demo.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class TalentPostSpecs {

    /** 상태 필터: ACTIVE / DELETED 등 */
    public static Specification<TalentPost> statusIs(TalentStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** 카테고리 필터 */
    public static Specification<TalentPost> categoryIs(TalentCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** 작성자 userId로 필터 */
    public static Specification<TalentPost> authoredByUserId(String userId) {
        return (root, query, cb) ->
                cb.equal(root.get("author").get("userId"), userId);
    }

    /** 태그 id로 필터 */
    public static Specification<TalentPost> tagIs(Long tagId) {
        return (root, query, cb) -> {
            // TalentPost.tags (ManyToMany 또는 OneToMany) 기준
            Join<TalentPost, Tag> tagJoin = root.join("tags", JoinType.LEFT);
            // 중복 방지
            query.distinct(true);
            return cb.equal(tagJoin.get("id"), tagId);
        };
    }

    /**
     * 🔍 통합 키워드 검색
     * - 게시글 제목(title)
     * - 게시글 내용(content)
     * - 작성자 이름(username)
     * - 작성자 아이디(userId)
     * - 작성자 전공(major)
     * - 태그 이름(tag.name)
     */
    public static Specification<TalentPost> keywordLike(String keyword) {
        String k = keyword == null ? null : keyword.trim();
        if (k == null || k.isEmpty()) {
            // 항상 true인 조건 (spec.and(...) 에 안전하게 쓰려고)
            return (root, query, cb) -> cb.conjunction();
        }

        String pattern = "%" + k.toLowerCase() + "%";

        return (root, query, cb) -> {
            // 작성자 조인
            var author = root.join("author", JoinType.LEFT);
            // 태그 조인
            Join<TalentPost, Tag> tagJoin = root.join("tags", JoinType.LEFT);

            // 태그 조인 때문에 중복 row 발생할 수 있어서 distinct 처리
            query.distinct(true);

            return cb.or(
                    // 제목
                    cb.like(cb.lower(root.get("title")), pattern),
                    // 내용
                    cb.like(cb.lower(root.get("content")), pattern),
                    // 작성자 이름(username)
                    cb.like(cb.lower(author.get("username")), pattern),
                    // 작성자 아이디(userId)
                    cb.like(cb.lower(author.get("userId")), pattern),
                    // 작성자 전공(major)
                    cb.like(cb.lower(author.get("major")), pattern),
                    // 태그 이름
                    cb.like(cb.lower(tagJoin.get("name")), pattern)
            );
        };
    }
}
