package com.grimgate.grimgate_backend.domain.owner.service;

import com.grimgate.grimgate_backend.domain.manager.entity.Manager;
import com.grimgate.grimgate_backend.domain.manager.repository.ManagerRepository;
import com.grimgate.grimgate_backend.domain.review.entity.Review;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewImageRepository;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewRepository;
import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReservationResponse;
import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReservationSearchRequest;
import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReservationStatsResponse;
import com.grimgate.grimgate_backend.domain.owner.dto.ReservationResultRequest;
import com.grimgate.grimgate_backend.domain.owner.dto.ReservationResultResponse;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.achievement.service.AchievementService;
import com.grimgate.grimgate_backend.domain.title.service.TitleService;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationStatsProjection;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeCreateRequest;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeCreateResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeUpdateRequest;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeUpdateResponse;
import com.grimgate.grimgate_backend.domain.theme.entity.Branch;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.BranchRepository;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import com.grimgate.grimgate_backend.global.S3.S3Uploader;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerService {
    private final ThemeRepository themeRepository;
    private final BranchRepository branchRepository;
    private final ManagerRepository managerRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final S3Uploader s3Uploader;
    private final TitleService titleService;
    private final AchievementService achievementService;

    // 테마 등록
    @Transactional
    public ThemeCreateResponse createTheme(ThemeCreateRequest request, MultipartFile thumbnail) {
        String thumbnailUrl = s3Uploader.upload(thumbnail, "themes");
        Long accountId = SecurityUtil.getCurrentAccountId();
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MANAGER_NOT_FOUND));
        Branch branch = branchRepository.findByManagerId(manager.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        Theme theme = Theme.builder()
                .branch(branch)
                .title(request.getTitle())
                .description(request.getDescription())
                .difficulty(request.getDifficulty())
                .horrorLevel(request.getHorrorLevel())
                .ageLimit(request.getAgeLimit())
                .playTime(request.getPlayTime())
                .minPeople(request.getMinPeople())
                .maxPeople(request.getMaxPeople())
                .price(request.getPrice())
                .rating(0.0)
                .reviewCount(0)
                .tags(request.getTags())
                .thumbnailUrl(thumbnailUrl)
                .build();

        Theme savedTheme = themeRepository.save(theme);
        return new ThemeCreateResponse(savedTheme.getId(), savedTheme.getCreatedAt());
    }

    // 테마 수정
    @Transactional
    public ThemeUpdateResponse updateTheme(Long themeId, ThemeUpdateRequest request, MultipartFile thumbnail) {

        Long accountId = SecurityUtil.getCurrentAccountId();
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(()-> new CustomException(ErrorCode.MANAGER_NOT_FOUND));
        Branch branch = branchRepository.findByManagerId(manager.getId())
                .orElseThrow(()-> new CustomException(ErrorCode.BRANCH_NOT_FOUND));
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(()-> new CustomException(ErrorCode.THEME_NOT_FOUND));

        // 본인 지점 테마인지 검증
        if (!theme.getBranch().getId().equals(branch.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // min이 max보다 크지 않도록 검증
        int minPeople = request.getMinPeople() != null
                ? request.getMinPeople()
                : theme.getMinPeople();

        int maxPeople = request.getMaxPeople() != null
                ? request.getMaxPeople()
                : theme.getMaxPeople();

        if (minPeople > maxPeople) {
            throw new CustomException(ErrorCode.INVALID_THEME_CAPACITY);
        }

        // 이미지 변경 요청이 있을 때만 업로드
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String thumbnailUrl = s3Uploader.upload(thumbnail, "themes");
            theme.updateThumbnail(thumbnailUrl);
        }

        theme.update(request);
        return new ThemeUpdateResponse(theme.getId(), theme.getUpdatedAt());
    }

    // 테마 삭제
    @Transactional
    public void deleteTheme(Long themeId) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(()-> new CustomException(ErrorCode.MANAGER_NOT_FOUND));
        Branch branch = branchRepository.findByManagerId(manager.getId())
                .orElseThrow(()->new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(()-> new CustomException(ErrorCode.THEME_NOT_FOUND));

        // 본인 지점 테마인지 검증
        if (!theme.getBranch().getId().equals(branch.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        List<Long> reviewIds = reviewRepository.findByThemeId(themeId)
                .stream()
                .map(Review::getId)
                .toList();

        reviewIds.forEach(reviewImageRepository::deleteByReviewId);
        // 테마 후기 삭제
        reviewRepository.deleteByThemeId(themeId);

        themeRepository.deleteById(themeId);
    }


    // 테마 전체 조회
    public List<ThemeResponse> getOwnerThemes() {
        Long accountId = SecurityUtil.getCurrentAccountId();
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(()-> new CustomException(ErrorCode.MANAGER_NOT_FOUND));
        Branch branch = branchRepository.findByManagerId(manager.getId())
                .orElseThrow(()-> new CustomException(ErrorCode.BRANCH_NOT_FOUND));
        return themeRepository.findByBranchId(branch.getId())
                .stream()
                .map(ThemeResponse::from)
                .collect(Collectors.toList());
    }

    // 사장님 예약 목록 검색
    @Transactional(readOnly = true)
    public Page<OwnerReservationResponse> searchReservations(
            OwnerReservationSearchRequest request,
            Pageable pageable
    ) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MANAGER_NOT_FOUND));
        Branch branch = branchRepository.findByManagerId(manager.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        return reservationRepository.findReservationsByBranchAndFilters(
                branch.getId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getThemeId(),
                request.getNickname(),
                request.getStatus(),
                pageable
        ).map(OwnerReservationResponse::from);
    }

    // 방탈출 결과 기록
    @Transactional
    public ReservationResultResponse recordReservationResult(Long reservationId, ReservationResultRequest request) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MANAGER_NOT_FOUND));
        Branch branch = branchRepository.findByManagerId(manager.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        // 본인 지점 예약인지 검증
        if (!reservation.getTimeSlot().getTheme().getBranch().getId().equals(branch.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // COMPLETED면 이미 결과 입력 완료
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new CustomException(ErrorCode.RESERVATION_ALREADY_COMPLETED);
        }

        // CONFIRMED 상태만 결과 입력 가능
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_CONFIRMED);
        }

        // 클리어 성공 시 clearTime 필수
        if (Boolean.TRUE.equals(request.getIsCleared()) && request.getClearTime() == null) {
            throw new CustomException(ErrorCode.CLEAR_TIME_REQUIRED);
        }

        // 실패 시 clearTime null 저장
        LocalTime clearTime = Boolean.TRUE.equals(request.getIsCleared()) ? request.getClearTime() : null;
        reservation.recordResult(request.getIsCleared(), clearTime);

        // 결과 기록 후 해당 회원의 칭호 갱신
        Member member = reservation.getMember();
        List<Reservation> memberReservations = reservationRepository.findByMemberWithTimeSlot(member);
        long totalPlayCount = titleService.calcTotalPlayCount(memberReservations);
        long clearedCount = titleService.calcClearedCount(memberReservations);
        double successRate = titleService.calcSuccessRate(totalPlayCount, clearedCount);

        titleService.findMatchingTitleId((int) totalPlayCount, (int) clearedCount, successRate)
                .ifPresent(member::updateTitleId);

        // 칭호 갱신 후 기본 업적 지급 (TOTAL_PLAY_COUNT, CLEAR_TIME_UNDER)
        long completedCount = memberReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .count();
        achievementService.grantResultAchievements(member, completedCount, request.getIsCleared(), clearTime);

        return ReservationResultResponse.from(reservation);
    }

    // 사장님 예약 요약 통계 조회
    @Transactional(readOnly = true)
    public OwnerReservationStatsResponse getReservationStats(LocalDate startDate, LocalDate endDate) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MANAGER_NOT_FOUND));
        Branch branch = branchRepository.findByManagerId(manager.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        LocalDate today = LocalDate.now();

        ReservationStatsProjection projection = reservationRepository.findReservationStats(
                branch.getId(),
                startDate,
                endDate,
                today
        );

        return OwnerReservationStatsResponse.builder()
                .totalCount(projection.getTotalCount())
                .todayCount(projection.getTodayCount())
                .completedCount(projection.getCompletedCount())
                .confirmedCount(projection.getConfirmedCount())
                .cancelledCount(projection.getCancelledCount())
                .build();
    }
}
