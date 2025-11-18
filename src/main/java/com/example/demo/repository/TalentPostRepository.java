// com.example.demo.repository.TalentPostRepository.java
package com.example.demo.repository;

import com.example.demo.entity.TalentPost;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TalentPostRepository extends JpaRepository<TalentPost, Long>, JpaSpecificationExecutor<TalentPost> {
    void deleteAllByAuthor(User author); // ✅ 추가: 작성자 기준 전체 삭제
}
