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

    // 재능 글 DB
    private final TalentPostRepository postRepo;
    // 유저 DB
    private final UserRepository userRepo;
    // 태그 DB
    private final TagRepository tagRepo;
    // 즐겨찾기 DB
    private final TalentFavoriteRepository talentFavoriteRepository;

    /* ========= 작은 유틸 메서드들 ========= */

    // 문자열 앞뒤 공백 제거하고, 비어 있으면 null 리턴
    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // 필수 값 체크 + 앞뒤 공백 제거
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

    // 생성 요청에서 태그 ID들을 하나의 리스트로 정리
    private List<Long> resolveTagIds(TalentPostCreateRequest req) {
        if (req.tagIds() != null && !req.tagIds().isEmpty()) return req.tagIds();
        if (req.tagId() != null) return List.of(req.tagId());
        throw new IllegalArgumentException("태그를 1개 이상 선택하세요.");
    }

    // 수정 요청에서 태그 ID들을 하나의 리스트로 정리
    private List<Long> resolveTagIds(TalentPostUpdateRequest req) {
        if (req.tagIds() != null && !req.tagIds().isEmpty()) return req.tagIds();
        if (req.tagId() != null) return List.of(req.tagId());
        throw new IllegalArgumentException("태그를 1개 이상 선택하세요.");
    }

    // 태그 ID로 실제 Tag 객체들을 DB에서 불러오고, 유효한지 검사
    private List<Tag> loadAndValidateTags(List<Long> ids, TalentCategory category) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("태그가 비어 있습니다.");
        }

        // 한 번에 여러 태그를 조회
        List<Tag> found = tagRepo.findAllById(ids);

        // 요청한 개수와 실제 찾은 개수가 다르면 잘못된 ID가 있다는 뜻
        if (found.size() != new HashSet<>(ids).size()) {
            throw new IllegalArgumentException("선택한 태그 중 존재하지 않는 항목이 있습니다.");
        }

        for (Tag t : found) {
            // 비활성화된 태그는 사용할 수 없음
            if (!t.isActive()) {
                throw new IllegalArgumentException("비활성화된 태그가 포함되어 있습니다: " + t.getName());
            }
            // 카테고리 검사 부분은 주석 처리 (현재는 사용 안 함)
        }

        // ID 기준으로 순서를 한 번 정리 (중복 제거)
        Map<Long, Tag> map = found.stream()
                .collect(Collectors.toMap(
                        Tag::getId,
                        x -> x,
                        (a, b) -> a,
                        LinkedHashMap::new)
                );
        return new ArrayList<>(map.values());
    }

    /**
     * 이미지 URL 리스트 정리
     * - null 제거
     * - 공백 제거
     * - 최대 10장까지만 사용
     */
    private List<String> normalizeImageUrls(List<String> urls) {
        if (urls == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String s : urls) {
            String t = trimOrNull(s);
            if (t != null) out.add(t);
            if (out.size() >= 10) break; // 최대 10장까지만
        }
        return out;
    }

    /* ========= 재능 글 비즈니스 로직 ========= */

    /**
     * 재능 글 생성
     * - 작성자 userId로 User 찾기
     * - 태그 유효성 검사
     * - 이미지 리스트/대표 이미지 정리
     */
    @Transactional
    public TalentPostResponse create(String authorUserId, TalentPostCreateRequest req) {
        // 작성자 유저 찾기
        User author = userRepo.findByUserId(authorUserId)
                .orElseThrow(() -> new IllegalArgumentException("작성자 계정을 찾을 수 없습니다."));

        // 태그 ID 정리 + 실제 태그 불러오기
        List<Long> tagIds = resolveTagIds(req);
        List<Tag> tags = loadAndValidateTags(tagIds, req.category());

        // 여러 장 이미지 처리
        List<String> urls = normalizeImageUrls(req.portfolioImageUrls());
        // 이전 버전 호환용 단일 이미지 필드
        String legacyMain = trimOrNull(req.portfolioImageUrl());

        // 새 리스트가 비어 있는데 단일 이미지가 있으면 → 단일 이미지만 사용
        if (urls.isEmpty() && legacyMain != null) {
            urls = List.of(legacyMain);
        } else if (!urls.isEmpty() && legacyMain == null) {
            // 리스트는 있는데 대표 이미지가 비어 있으면 → 첫 번째 이미지를 대표 이미지로 사용
            legacyMain = urls.get(0);
        }

        // 새 재능 글 엔티티 만들기
        TalentPost post = TalentPost.builder()
                .title(requireAndTrim(req.title(), "제목"))
                .content(requireAndTrim(req.content(), "내용"))
                .category(req.category())
                .extraNote(trimOrNull(req.extraNote()))
                .portfolioImageUrl(legacyMain)     // 대표 이미지
                .portfolioImageUrls(urls)          // 여러 장 이미지
                .price(req.price() == null ? 0 : req.price())
                .location(trimOrNull(req.location()))
                .status(TalentStatus.ACTIVE)       // 처음에는 활성 상태
                .views(0L)
                .likesCount(0L)
                .author(author)                    // 작성자 연결
                .build();

        // 태그 연결
        post.setTags(tags);

        // 저장 후 응답 DTO로 변경해서 반환
        return toResponse(postRepo.save(post));
    }

    /**
     * 재능 글 검색 (실제 발표에서는 페이징/스펙 부분은 간단히만 언급해도 됨)
     */
    @Transactional(readOnly = true)
    public Page<TalentPostListItem> search(
            String q,
            TalentCategory category,
            String authorUserId,
            Long tagId,
            Pageable pageable
    ) {
        // 기본 조건: ACTIVE 상태인 글만
        Specification<TalentPost> spec = statusIs(TalentStatus.ACTIVE);

        // 키워드 검색
        if (q != null && !q.isBlank()) spec = spec.and(keywordLike(q));
        // 카테고리 필터
        if (category != null) spec = spec.and(categoryIs(category));
        // 작성자 필터
        if (authorUserId != null && !authorUserId.isBlank()) spec = spec.and(authoredByUserId(authorUserId));
        // 태그 필터
        if (tagId != null) spec = spec.and(tagIs(tagId));

        // 조건에 맞는 글들을 찾고, 목록용 DTO로 변환
        return postRepo.findAll(spec, pageable).map(this::toListItem);
    }

    /**
     * 재능 글 상세 조회 + 조회수 1 증가
     */
    @Transactional
    public TalentPostResponse getAndIncreaseView(Long id) {
        // 글 찾기
        TalentPost p = postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // 삭제된 글이면 예외
        if (p.getStatus() == TalentStatus.DELETED) {
            throw new IllegalArgumentException("삭제된 게시글입니다.");
        }

        // 조회수 증가
        p.increaseViews();

        // 응답 DTO로 변환
        return toResponse(p);
    }

    /**
     * 재능 글 수정
     * - 본인 글인지 확인
     * - 태그/이미지/내용 수정
     */
    @Transactional
    public TalentPostResponse update(Long id, String editorUserId, TalentPostUpdateRequest req) {
        TalentPost p = postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // 수정하려는 사람이 작성자인지 확인
        if (!p.getAuthor().getUserId().equals(editorUserId)) {
            throw new IllegalArgumentException("본인 게시글만 수정할 수 있습니다.");
        }

        // 태그 정리 + 유효성 검사
        List<Long> tagIds = resolveTagIds(req);
        List<Tag> tags = loadAndValidateTags(tagIds, req.category());

        // 이미지들 정리
        List<String> urls = normalizeImageUrls(req.portfolioImageUrls());
        String legacyMain = trimOrNull(req.portfolioImageUrl());
        if (urls.isEmpty() && legacyMain != null) {
            urls = List.of(legacyMain);
        } else if (!urls.isEmpty() && legacyMain == null) {
            legacyMain = urls.get(0);
        }

        // 필드들 업데이트
        p.setTitle(requireAndTrim(req.title(), "제목"));
        p.setContent(requireAndTrim(req.content(), "내용"));
        p.setCategory(req.category());
        p.setTags(tags);
        p.setExtraNote(trimOrNull(req.extraNote()));
        p.setPortfolioImageUrl(legacyMain);
        p.setPortfolioImageUrls(urls);
        p.setPrice(req.price() == null ? 0 : req.price());
        p.setLocation(trimOrNull(req.location()));

        // 상태 값이 들어온 경우에만 변경
        if (req.status() != null) {
            p.setStatus(TalentStatus.valueOf(req.status()));
        }

        return toResponse(p);
    }

    /**
     * 재능 글 삭제 (soft delete)
     * - 실제로 DB에서 지우지 않고 "DELETED" 상태로만 바꿈
     */
    @Transactional
    public void softDelete(Long id, String requesterUserId) {
        TalentPost p = postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        if (!p.getAuthor().getUserId().equals(requesterUserId)) {
            throw new IllegalArgumentException("본인 게시글만 삭제할 수 있습니다.");
        }
        p.setStatus(TalentStatus.DELETED);
    }

    /* ========= 즐겨찾기 관련 로직 ========= */

    /**
     * 현재 유저가 이 글을 즐겨찾기 했는지 여부
     */
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
     * - 즐겨찾기가 없으면 추가 → true
     * - 이미 있으면 삭제 → false
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
                    // 이미 즐겨찾기 되어 있으면 삭제
                    talentFavoriteRepository.delete(existing);
                    return false;   // 해제 상태
                })
                .orElseGet(() -> {
                    // 없으면 새로 즐겨찾기 추가
                    talentFavoriteRepository.save(new TalentFavorite(user, post));
                    return true;    // 즐겨찾기 된 상태
                });
    }

    /**
     * 내 즐겨찾기 목록 조회
     * - TalentFavorite → TalentPost → 리스트용 DTO로 변환
     */
    @Transactional(readOnly = true)
    public Page<TalentPostListItem> getMyFavorites(String currentUserId, Pageable pageable) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User user = userRepo.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<TalentFavorite> favorites = talentFavoriteRepository.findByUser(user, pageable);

        return favorites.map(fav -> toListItem(fav.getPost()));
    }

    /* ========= DTO 변환 ========= */

    // 상세 응답용 DTO로 변환
    private TalentPostResponse toResponse(TalentPost p) {
        List<Long> tagIds = p.getTags().stream().map(Tag::getId).toList();
        List<String> tagNames = p.getTags().stream().map(Tag::getName).toList();

        // LAZY 컬렉션을 일반 리스트로 한 번 복사해줌
        List<String> portfolioImageUrls =
                p.getPortfolioImageUrls() == null
                        ? List.of()
                        : new ArrayList<>(p.getPortfolioImageUrls());

        // 대표 이미지는 첫 번째 장 (없으면 null)
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
                cover,                 // 대표 이미지
                portfolioImageUrls,    // 여러 장 이미지
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

    // 목록 카드용 DTO로 변환
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
