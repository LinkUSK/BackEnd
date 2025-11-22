package com.example.demo.service;

import com.example.demo.dto.talent.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.demo.repository.TalentPostSpecs.*;

@Service
@RequiredArgsConstructor
public class TalentPostService {

    private final TalentPostRepository postRepo;
    private final UserRepository userRepo;
    private final TagRepository tagRepo;
    private final TalentFavoriteRepository talentFavoriteRepository;   // ⭐ 즐겨찾기

    /* ========= 작은 유틸 ========= */

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String requireAndTrim(String s, String fieldName) {
        if (s == null) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수 값입니다.");
        }
        String t = s.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 비어 있을 수 없습니다.");
        }
        return t;
    }

    private List<Long> resolveTagIds(TalentPostCreateRequest req) {
        if (req.tagIds() != null && !req.tagIds().isEmpty()) return req.tagIds();
        if (req.tagId() != null) return List.of(req.tagId());
        throw new IllegalArgumentException("태그를 1개 이상 선택하세요.");
    }

    private List<Long> resolveTagIds(TalentPostUpdateRequest req) {
        if (req.tagIds() != null && !req.tagIds().isEmpty()) return req.tagIds();
        if (req.tagId() != null) return List.of(req.tagId());
        throw new IllegalArgumentException("태그를 1개 이상 선택하세요.");
    }

    private List<Tag> loadAndValidateTags(List<Long> ids, TalentCategory category) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("태그가 비어 있습니다.");
        }
        List<Tag> found = tagRepo.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            throw new IllegalArgumentException("선택한 태그 중 존재하지 않는 항목이 있습니다.");
        }
        for (Tag t : found) {
            if (!t.isActive()) {
                throw new IllegalArgumentException("비활성화된 태그가 포함되어 있습니다: " + t.getName());
            }
            // ❌ 기존: 카테고리 enum 비교하던 부분 제거
            // if (t.getCategory() != category) {
            //     throw new IllegalArgumentException("카테고리와 태그의 카테고리가 일치하지 않습니다. (" + t.getName() + ")");
            // }
        }
        Map<Long, Tag> map = found.stream()
                .collect(Collectors.toMap(
                        Tag::getId,
                        x -> x,
                        (a, b) -> a,
                        LinkedHashMap::new)
                );
        return new ArrayList<>(map.values());
    }

    // ✅ 이미지 URL 리스트 정리 (공백 제거 + null 제거 + 개수 제한)
    private List<String> normalizeImageUrls(List<String> urls) {
        if (urls == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String s : urls) {
            String t = trimOrNull(s);
            if (t != null) out.add(t);
            if (out.size() >= 10) break; // 최대 10장
        }
        return out;
    }

    /* ========= 비즈니스 로직 ========= */

    @Transactional
    public TalentPostResponse create(String authorUserId, TalentPostCreateRequest req) {
        User author = userRepo.findByUserId(authorUserId)
                .orElseThrow(() -> new IllegalArgumentException("작성자 계정을 찾을 수 없습니다."));

        List<Long> tagIds = resolveTagIds(req);
        List<Tag> tags = loadAndValidateTags(tagIds, req.category());

        // ✅ 이미지 처리
        List<String> urls = normalizeImageUrls(req.portfolioImageUrls());
        String legacyMain = trimOrNull(req.portfolioImageUrl());

        if (urls.isEmpty() && legacyMain != null) {
            urls = List.of(legacyMain);
        } else if (!urls.isEmpty() && legacyMain == null) {
            legacyMain = urls.get(0);
        }

        TalentPost post = TalentPost.builder()
                .title(requireAndTrim(req.title(), "제목"))
                .content(requireAndTrim(req.content(), "내용"))
                .category(req.category())
                .extraNote(trimOrNull(req.extraNote()))
                .portfolioImageUrl(legacyMain)
                .portfolioImageUrls(urls)
                .price(req.price() == null ? 0 : req.price())
                .location(trimOrNull(req.location()))
                .status(TalentStatus.ACTIVE)
                .views(0L)
                .likesCount(0L)
                .author(author)
                .build();

        post.setTags(tags);
        return toResponse(postRepo.save(post));
    }

    @Transactional(readOnly = true)
    public Page<TalentPostListItem> search(
            String q,
            TalentCategory category,
            String authorUserId,
            Long tagId,
            Pageable pageable
    ) {
        Specification<TalentPost> spec = statusIs(TalentStatus.ACTIVE);
        if (q != null && !q.isBlank()) spec = spec.and(keywordLike(q));
        if (category != null) spec = spec.and(categoryIs(category));
        if (authorUserId != null && !authorUserId.isBlank()) spec = spec.and(authoredByUserId(authorUserId));
        if (tagId != null) spec = spec.and(tagIs(tagId));

        return postRepo.findAll(spec, pageable).map(this::toListItem);
    }

    @Transactional
    public TalentPostResponse getAndIncreaseView(Long id) {
        TalentPost p = postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        if (p.getStatus() == TalentStatus.DELETED) {
            throw new IllegalArgumentException("삭제된 게시글입니다.");
        }
        p.increaseViews();
        return toResponse(p);
    }

    @Transactional
    public TalentPostResponse update(Long id, String editorUserId, TalentPostUpdateRequest req) {
        TalentPost p = postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        if (!p.getAuthor().getUserId().equals(editorUserId)) {
            throw new IllegalArgumentException("본인 게시글만 수정할 수 있습니다.");
        }

        List<Long> tagIds = resolveTagIds(req);
        List<Tag> tags = loadAndValidateTags(tagIds, req.category());

        // ✅ 이미지 처리
        List<String> urls = normalizeImageUrls(req.portfolioImageUrls());
        String legacyMain = trimOrNull(req.portfolioImageUrl());
        if (urls.isEmpty() && legacyMain != null) {
            urls = List.of(legacyMain);
        } else if (!urls.isEmpty() && legacyMain == null) {
            legacyMain = urls.get(0);
        }

        p.setTitle(requireAndTrim(req.title(), "제목"));
        p.setContent(requireAndTrim(req.content(), "내용"));
        p.setCategory(req.category());
        p.setTags(tags);
        p.setExtraNote(trimOrNull(req.extraNote()));
        p.setPortfolioImageUrl(legacyMain);
        p.setPortfolioImageUrls(urls);
        p.setPrice(req.price() == null ? 0 : req.price());
        p.setLocation(trimOrNull(req.location()));

        if (req.status() != null) {
            p.setStatus(TalentStatus.valueOf(req.status()));
        }

        return toResponse(p);
    }

    @Transactional
    public void softDelete(Long id, String requesterUserId) {
        TalentPost p = postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        if (!p.getAuthor().getUserId().equals(requesterUserId)) {
            throw new IllegalArgumentException("본인 게시글만 삭제할 수 있습니다.");
        }
        p.setStatus(TalentStatus.DELETED);
    }

    /* ========= 즐겨찾기 관련 ========= */

    /** 현재 유저가 postId 글을 즐겨찾기 했는지 */
    @Transactional(readOnly = true)
    public boolean isFavorite(String currentUserId, Long postId) {
        if (currentUserId == null) return false;

        User user = userRepo.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        TalentPost post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        return talentFavoriteRepository.existsByUserAndPost(user, post);
    }

    /**
     * 즐겨찾기 토글
     * - 없으면 추가 후 true 반환
     * - 있으면 삭제 후 false 반환
     */
    @Transactional
    public boolean toggleFavorite(String currentUserId, Long postId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User user = userRepo.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        TalentPost post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        return talentFavoriteRepository.findByUserAndPost(user, post)
                .map(existing -> {
                    talentFavoriteRepository.delete(existing);
                    return false;   // 해제 상태
                })
                .orElseGet(() -> {
                    talentFavoriteRepository.save(new TalentFavorite(user, post));
                    return true;    // 즐겨찾기 된 상태
                });
    }

    /** ⭐ 내 즐겨찾기 목록 조회 (재능글 카드 리스트 형태) */
    @Transactional(readOnly = true)
    public Page<TalentPostListItem> getMyFavorites(String currentUserId, Pageable pageable) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User user = userRepo.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<TalentFavorite> favorites = talentFavoriteRepository.findByUser(user, pageable);

        // 즐겨찾기 엔티티 -> TalentPost -> TalentPostListItem 으로 변환
        return favorites.map(fav -> toListItem(fav.getPost()));
    }

    /* ========= DTO 변환 ========= */

    private TalentPostResponse toResponse(TalentPost p) {
        List<Long> tagIds = p.getTags().stream().map(Tag::getId).toList();
        List<String> tagNames = p.getTags().stream().map(Tag::getName).toList();

        // 🔹 여기서 LAZY 컬렉션을 한 번 복사해서 일반 리스트로 만들어줌
        List<String> portfolioImageUrls =
                p.getPortfolioImageUrls() == null
                        ? List.of()
                        : new ArrayList<>(p.getPortfolioImageUrls());

        // 대표 이미지는 첫 번째 장 사용 (없으면 null)
        String cover =
                portfolioImageUrls.isEmpty() ? null : portfolioImageUrls.get(0);

        return new TalentPostResponse(
                p.getId(),
                p.getTitle(),
                p.getContent(),
                p.getCategory(),
                tagIds,
                tagNames,
                p.getExtraNote(),
                cover,                 // 단일 대표 이미지
                portfolioImageUrls,    // 여러 장
                p.getPrice(),
                p.getLocation(),
                p.getAuthor().getUserId(),
                p.getAuthor().getUsername(),
                p.getAuthor().getMajor(),
                p.getAuthor().getProfileImageUrl(),
                p.getViews(),
                p.getLikesCount(),
                p.getStatus().name(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private TalentPostListItem toListItem(TalentPost p) {
        List<Long> tagIds = p.getTags().stream().map(Tag::getId).toList();
        List<String> tagNames = p.getTags().stream().map(Tag::getName).toList();

        return new TalentPostListItem(
                p.getId(),
                p.getTitle(),
                p.getCategory(),
                tagIds,
                tagNames,
                p.getPrice(),
                p.getLocation(),
                p.getAuthor().getUserId(),
                p.getAuthor().getUsername(),
                p.getAuthor().getMajor(),
                p.getAuthor().getProfileImageUrl(),
                p.getViews(),
                p.getCreatedAt()
        );
    }
}
