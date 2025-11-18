package com.example.demo.repository;

import com.example.demo.entity.Tag;
import com.example.demo.entity.TalentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByCategoryAndActiveTrueOrderByNameAsc(TalentCategory category);
    Optional<Tag> findByCategoryAndNameIgnoreCase(TalentCategory category, String name);
}
