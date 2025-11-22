// com.example.demo.repository.TagRepository.java
package com.example.demo.repository;

import com.example.demo.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    // 🔥 카테고리 문자열 기준 조회
    List<Tag> findByCategoryAndActiveTrueOrderByNameAsc(String category);

    Optional<Tag> findByCategoryAndNameIgnoreCase(String category, String name);

    // 🔥 활성 태그가 사용하고 있는 카테고리 목록 (중복 제거)
    @Query("select distinct t.category from Tag t where t.active = true order by t.category asc")
    List<String> findDistinctActiveCategories();
}
