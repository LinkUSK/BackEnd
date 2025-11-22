// com.example.demo.config.DataSeed.java
package com.example.demo.config;

import com.example.demo.entity.Tag;
import com.example.demo.repository.TagRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeed {

    @Bean
    CommandLineRunner seedTags(TagRepository repo) {
        return args -> {
            // 🔥 여기서는 기본으로 보여줄 카테고리/태그만 몇 개 넣어줌
            List.of(
                    new Object[]{"PHOTO", List.of("포스터", "로고", "프로필", "졸업사진")},
                    new Object[]{"MODEL", List.of("헤어", "피팅", "메이크업")},
                    new Object[]{"PROGRAMMING", List.of("웹개발", "앱개발", "데이터분석")}
            ).forEach(entry -> {
                String cat = (String) entry[0];
                ((List<String>) entry[1]).forEach(n ->
                        repo.findByCategoryAndNameIgnoreCase(cat, n).orElseGet(() ->
                                repo.save(
                                        Tag.builder()
                                                .category(cat)
                                                .name(n)
                                                .active(true)
                                                .build()
                                )
                        )
                );
            });
        };
    }
}
