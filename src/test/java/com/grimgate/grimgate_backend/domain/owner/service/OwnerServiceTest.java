package com.grimgate.grimgate_backend.domain.owner.service;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.manager.entity.Manager;
import com.grimgate.grimgate_backend.domain.manager.repository.ManagerRepository;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewImageRepository;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewRepository;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeCreateRequest;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeCreateResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeUpdateRequest;
import com.grimgate.grimgate_backend.domain.theme.entity.Branch;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.BranchRepository;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReservationSearchRequest;
import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReservationResponse;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.global.S3.S3Uploader;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class OwnerServiceTest {

    @InjectMocks
    private OwnerService ownerService;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock private S3Uploader s3Uploader;

    @Test
    @DisplayName("테마 등록 성공")
    void createTheme_success() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {

            securityUtil.when(SecurityUtil::getCurrentAccountId)
                    .thenReturn(1L);

            Account account = Account.builder()
                    .id(1L)
                    .build();

            Manager manager = Manager.builder()
                    .id(1L)
                    .account(account)
                    .build();

            Branch branch = Branch.builder()
                    .id(1L)
                    .build();

            ThemeCreateRequest request = new ThemeCreateRequest();

            when(managerRepository.findByAccount_Id(any()))
                    .thenReturn(Optional.of(manager));

            when(branchRepository.findByManagerId(any()))
                    .thenReturn(Optional.of(branch));

            when(themeRepository.save(any(Theme.class)))
                    .thenAnswer(invocation -> {
                        Theme t = invocation.getArgument(0);
                        return Theme.builder()
                                .id(1L)
                                .branch(t.getBranch())
                                .createdAt(LocalDateTime.now())
                                .build();
                    });

            when(s3Uploader.upload(any(), any())).thenReturn("https://test-url.jpg");
            ThemeCreateResponse response = ownerService.createTheme(request, null);
            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.createdAt()).isNotNull();

            verify(themeRepository, times(1))
                    .save(any(Theme.class));
        }
    }

    @Test
    @DisplayName("테마 수정 성공")
    void updateTheme_success() {
        // given
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentAccountId).thenReturn(1L);

            Manager manager = Manager.builder().id(1L).build();
            Branch branch = Branch.builder().id(1L).build();
            Theme theme = Theme.builder().branch(branch).minPeople(2).maxPeople(6).build();
            ThemeUpdateRequest request = new ThemeUpdateRequest();

            when(managerRepository.findByAccount_Id(any())).thenReturn(Optional.of(manager));
            when(branchRepository.findByManagerId(any())).thenReturn(Optional.of(branch));
            when(themeRepository.findById(any())).thenReturn(Optional.of(theme));

            // when
            ownerService.updateTheme(1L, request, null);

            // then
            verify(themeRepository, times(1)).findById(1L);
        }
    }

    @Test
    @DisplayName("테마 삭제 성공")
    void deleteTheme_success() {
        // given
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentAccountId).thenReturn(1L);

            Manager manager = Manager.builder().id(1L).build();
            Branch branch = Branch.builder().id(1L).build();
            Theme theme = Theme.builder().branch(branch).build();

            when(managerRepository.findByAccount_Id(any())).thenReturn(Optional.of(manager));
            when(branchRepository.findByManagerId(any())).thenReturn(Optional.of(branch));
            when(themeRepository.findById(any())).thenReturn(Optional.of(theme));
            when(reviewRepository.findByThemeId(any())).thenReturn(Collections.emptyList());

            // when
            ownerService.deleteTheme(1L);

            // then
            verify(themeRepository, times(1)).deleteById(1L);
        }
    }

    @Test
    @DisplayName("다른 지점 테마 수정, 삭제 실패")
    void updateTheme_forbidden() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentAccountId).thenReturn(1L);

            Manager manager = Manager.builder().id(1L).build();
            Branch myBranch = Branch.builder().id(1L).build();      // 내 지점
            Branch otherBranch = Branch.builder().id(2L).build();   // 다른 지점
            Theme theme = Theme.builder().branch(otherBranch).build(); // 다른 지점 테마
            ThemeUpdateRequest request = new ThemeUpdateRequest();

            when(managerRepository.findByAccount_Id(any())).thenReturn(Optional.of(manager));
            when(branchRepository.findByManagerId(any())).thenReturn(Optional.of(myBranch));
            when(themeRepository.findById(any())).thenReturn(Optional.of(theme));

            // then - 예외 발생해야 함
            assertThrows(CustomException.class, () -> ownerService.updateTheme(1L, request, null));
            assertThrows(CustomException.class, () -> ownerService.deleteTheme(1L));
        }
    }

    @Test
    @DisplayName("사장님 예약 목록 검색 성공")
    void searchReservations_success() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // given
            securityUtil.when(SecurityUtil::getCurrentAccountId).thenReturn(1L);

            Account managerAccount = Account.builder().id(1L).build();
            Manager manager = Manager.builder().id(10L).account(managerAccount).build();
            Branch branch = Branch.builder().id(100L).build();

            OwnerReservationSearchRequest searchRequest = OwnerReservationSearchRequest.builder()
                    .startDate(LocalDate.of(2026, 6, 1))
                    .endDate(LocalDate.of(2026, 6, 30))
                    .build();
            Pageable pageable = PageRequest.of(0, 10);

            Theme theme = Theme.builder().id(200L).title("테마1").build();
            TimeSlot timeSlot = TimeSlot.builder()
                    .id(300L)
                    .slotDate(LocalDate.of(2026, 6, 5))
                    .startTime(LocalTime.of(14, 0))
                    .theme(theme)
                    .build();
            Account memberAccount = Account.builder().nickname("예약자1").phone("010-1234-5678").build();
            Member member = Member.builder().id(400L).account(memberAccount).build();

            Reservation reservation = Reservation.builder()
                    .id(500L)
                    .timeSlot(timeSlot)
                    .member(member)
                    .peopleCount(3)
                    .status(ReservationStatus.CONFIRMED)
                    .isCleared(true)
                    .clearTime(LocalTime.of(0, 45, 30))
                    .build();

            Page<Reservation> reservationPage = new PageImpl<>(List.of(reservation), pageable, 1);

            when(managerRepository.findByAccount_Id(1L)).thenReturn(Optional.of(manager));
            when(branchRepository.findByManagerId(10L)).thenReturn(Optional.of(branch));
            when(reservationRepository.findReservationsByBranchAndFilters(
                    eq(100L), any(), any(), any(), any(), any(), eq(pageable)
            )).thenReturn(reservationPage);

            // when
            Page<OwnerReservationResponse> result = ownerService.searchReservations(searchRequest, pageable);

            // then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            OwnerReservationResponse response = result.getContent().get(0);
            assertEquals(500L, response.getReservationId());
            assertEquals("테마1", response.getThemeTitle());
            assertEquals("예약자1", response.getNickname());
            assertEquals("010-1234-5678", response.getPhone());
            assertEquals(3, response.getPeopleCount());
            assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
            assertEquals("성공 (45:30)", response.getEscapeResult());
        }
    }

    @Test
    @DisplayName("사장님 예약 목록 검색 - blank 닉네임은 null로 변환되어 전달된다")
    void searchReservations_blankNickname_success() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // given
            securityUtil.when(SecurityUtil::getCurrentAccountId).thenReturn(1L);

            Account managerAccount = Account.builder().id(1L).build();
            Manager manager = Manager.builder().id(10L).account(managerAccount).build();
            Branch branch = Branch.builder().id(100L).build();

            OwnerReservationSearchRequest searchRequest = OwnerReservationSearchRequest.builder()
                    .nickname("   ") // blank string
                    .build();
            Pageable pageable = PageRequest.of(0, 10);

            Theme theme = Theme.builder().id(200L).title("테마1").build();
            TimeSlot timeSlot = TimeSlot.builder()
                    .id(300L)
                    .slotDate(LocalDate.of(2026, 6, 5))
                    .startTime(LocalTime.of(14, 0))
                    .theme(theme)
                    .build();
            Account memberAccount = Account.builder().nickname("예약자1").phone("010-1234-5678").build();
            Member member = Member.builder().id(400L).account(memberAccount).build();

            Reservation reservation = Reservation.builder()
                    .id(500L)
                    .timeSlot(timeSlot)
                    .member(member)
                    .peopleCount(3)
                    .status(ReservationStatus.CONFIRMED)
                    .isCleared(true)
                    .clearTime(LocalTime.of(0, 45, 30))
                    .build();

            Page<Reservation> reservationPage = new PageImpl<>(List.of(reservation), pageable, 1);

            when(managerRepository.findByAccount_Id(1L)).thenReturn(Optional.of(manager));
            when(branchRepository.findByManagerId(10L)).thenReturn(Optional.of(branch));
            // 닉네임이 null로 바인딩되어 Repository로 들어가는지 검증
            when(reservationRepository.findReservationsByBranchAndFilters(
                    eq(100L), any(), any(), any(), eq(null), any(), eq(pageable)
            )).thenReturn(reservationPage);

            // when
            Page<OwnerReservationResponse> result = ownerService.searchReservations(searchRequest, pageable);

            // then
            assertNotNull(result);
            verify(reservationRepository).findReservationsByBranchAndFilters(
                    eq(100L), any(), any(), any(), eq(null), any(), eq(pageable)
            );
        }
    }
}