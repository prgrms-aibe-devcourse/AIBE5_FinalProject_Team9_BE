package com.grimgate.grimgate_backend.domain.auth.service;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.account.entity.Role;
import com.grimgate.grimgate_backend.domain.account.repository.AccountRepository;
import com.grimgate.grimgate_backend.domain.auth.dto.*;
import com.grimgate.grimgate_backend.domain.auth.entity.RefreshToken;
import com.grimgate.grimgate_backend.domain.auth.repository.RefreshTokenRepository;
import com.grimgate.grimgate_backend.domain.manager.entity.Manager;
import com.grimgate.grimgate_backend.domain.manager.repository.ManagerRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.entity.ProfileCharacter;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.member.repository.ProfileCharacterRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.Branch;
import com.grimgate.grimgate_backend.domain.theme.repository.BranchRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import com.grimgate.grimgate_backend.global.security.JwtProvider;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final ManagerRepository managerRepository;
    private final ProfileCharacterRepository profileCharacterRepository;
    private final BranchRepository branchRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    // Access Token 블랙리스트 Redis 키 접두사
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // 액세스 토큰 만료시간 (초): 30분
    private static final long ACCESS_TOKEN_EXPIRES_IN = 1800L;
    // 리프레시 토큰 TTL (초): 7일 / 30일
    private static final long REFRESH_TOKEN_TTL_DEFAULT = 604800L;
    private static final long REFRESH_TOKEN_TTL_REMEMBER_ME = 2592000L;

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request, Role role) {
        // 이메일 중복 체크
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 닉네임 중복 체크
        if (accountRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // 약관 동의 확인
        if (!request.isTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_NOT_AGREED);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Account 저장
        Account account = Account.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .password(encodedPassword)
                .phone(request.getPhone())
                .role(role)
                .gender(request.getGender())
                .age(request.getAge())
                .notificationEnabled(request.isMarketingAgreed())
                .build();

        accountRepository.save(account);

        // 역할에 따라 Member 또는 Manager 저장
        if (role == Role.MEMBER) {
            // 기본 프로필 캐릭터(id=1) 조회
            ProfileCharacter defaultCharacter = profileCharacterRepository.findById(1L)
                    .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_CHARACTER_NOT_FOUND));

            Member member = Member.builder()
                    .account(account)
                    .profileCharacter(defaultCharacter)
                    .build();

            memberRepository.save(member);
        } else if (role == Role.MANAGER) {
            Manager manager = Manager.builder()
                    .account(account)
                    .build();

            managerRepository.save(manager);
        }

        // 토큰 생성 (회원가입 즉시 로그인 처리)
        String accessToken = jwtProvider.generateAccessToken(account.getId(), account.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(account.getId());

        // Redis에 리프레시 토큰 저장 (기본 TTL: 7일)
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(String.valueOf(account.getId()))
                .accountId(account.getId())
                .token(refreshToken)
                .ttl(REFRESH_TOKEN_TTL_DEFAULT)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return SignupResponse.builder()
                .id(account.getId())
                .nickname(account.getNickname())
                .email(account.getEmail())
                .createdAt(account.getCreatedAt())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRES_IN)
                .build();
    }

    // 매니저 회원가입 (지점 정보 포함)
    @Transactional
    public SignupResponse signupManager(ManagerSignupRequest request) {
        // 이메일 중복 체크
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 닉네임 중복 체크
        if (accountRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // 약관 동의 확인
        if (!request.isTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_NOT_AGREED);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Account 저장
        Account account = Account.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .password(encodedPassword)
                .phone(request.getPhone())
                .role(Role.MANAGER)
                .gender(request.getGender())
                .age(request.getAge())
                .notificationEnabled(request.isMarketingAgreed())
                .build();

        accountRepository.save(account);

        // Manager 저장
        Manager manager = Manager.builder()
                .account(account)
                .build();

        managerRepository.save(manager);

        // branchCode 자동생성 (UUID 앞 8자리 대문자)
        String branchCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Branch 저장
        Branch branch = Branch.builder()
                .managerId(manager.getId())
                .branchCode(branchCode)
                .storeName(request.getStoreName())
                .branchName(request.getBranchName())
                .region(request.getRegion())
                .address(request.getAddress())
                .phone(request.getBranchPhone())
                .operatingHours("")
                .build();

        branchRepository.save(branch);

        // 토큰 생성 (회원가입 즉시 로그인 처리)
        String accessToken = jwtProvider.generateAccessToken(account.getId(), account.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(account.getId());

        // Redis에 리프레시 토큰 저장 (기본 TTL: 7일)
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(String.valueOf(account.getId()))
                .accountId(account.getId())
                .token(refreshToken)
                .ttl(REFRESH_TOKEN_TTL_DEFAULT)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return SignupResponse.builder()
                .id(account.getId())
                .nickname(account.getNickname())
                .email(account.getEmail())
                .createdAt(account.getCreatedAt())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRES_IN)
                .build();
    }

    // 로그인
    @Transactional
    public TokenResponse login(LoginRequest request, Role role) {
        // 이메일로 계정 조회 (@SQLRestriction으로 탈퇴 계정 자동 제외)
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 역할 일치 확인
        if (!account.getRole().equals(role)) {
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 토큰 생성
        String accessToken = jwtProvider.generateAccessToken(account.getId(), account.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(account.getId());

        // Redis TTL 결정
        long ttl = request.isRememberMe() ? REFRESH_TOKEN_TTL_REMEMBER_ME : REFRESH_TOKEN_TTL_DEFAULT;

        // Redis에 리프레시 토큰 저장 (key: accountId)
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(String.valueOf(account.getId()))
                .accountId(account.getId())
                .token(refreshToken)
                .ttl(ttl)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRES_IN)
                .build();
    }

    // 토큰 재발급 (RTR 방식)
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        // 토큰 유효성 검증
        if (!jwtProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // accountId 추출
        Long accountId = jwtProvider.getAccountId(token);

        // Redis에서 저장된 리프레시 토큰 조회
        RefreshToken stored = refreshTokenRepository.findById(String.valueOf(accountId))
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 저장된 값과 요청 토큰 일치 확인
        if (!stored.getToken().equals(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 실제 남은 TTL 조회 (초 단위)
        Long remainingTtl = redisTemplate.getExpire("refresh_token:" + accountId, TimeUnit.SECONDS);

        // 기존 Redis 키 삭제 (RTR)
        refreshTokenRepository.delete(stored);

        // 계정 조회 (역할 확인용)
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 새 토큰 발급
        String newAccessToken = jwtProvider.generateAccessToken(accountId, account.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(accountId);

        // Redis에 새 리프레시 토큰 저장 (실제 남은 TTL 유지, 조회 실패 시 원래 TTL 사용)
        long ttlToApply = (remainingTtl != null && remainingTtl > 0) ? remainingTtl : stored.getTtl();
        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .id(String.valueOf(accountId))
                .accountId(accountId)
                .token(newRefreshToken)
                .ttl(ttlToApply)
                .build();

        refreshTokenRepository.save(newRefreshTokenEntity);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRES_IN)
                .build();
    }

    // 로그아웃
    @Transactional
    public String logout(RefreshTokenRequest request, String accessToken) {
        String refreshToken = request.getRefreshToken();

        // 리프레시 토큰 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // accountId 추출
        Long accountId = jwtProvider.getAccountId(refreshToken);

        // Redis에서 리프레시 토큰 삭제
        refreshTokenRepository.deleteById(String.valueOf(accountId));

        // Access Token 블랙리스트 등록 (남은 만료 시간만큼 TTL 설정)
        if (StringUtils.hasText(accessToken)) {
            long remainingMs = jwtProvider.getRemainingExpiration(accessToken);
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + accessToken,
                        "logout",
                        remainingMs,
                        TimeUnit.MILLISECONDS
                );
            }
        }

        return "로그아웃 되었습니다.";
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(Long accountId, String accessToken) {
        // Redis에서 리프레시 토큰 삭제 (logout() 패턴 재사용)
        refreshTokenRepository.deleteById(String.valueOf(accountId));

        // Access Token 블랙리스트 등록 (logout() 패턴 재사용)
        if (StringUtils.hasText(accessToken)) {
            long remainingMs = jwtProvider.getRemainingExpiration(accessToken);
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + accessToken,
                        "withdraw",
                        remainingMs,
                        TimeUnit.MILLISECONDS
                );
            }
        }

        // 계정 조회 후 탈퇴 처리 (soft delete + 이메일 변조)
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.withdraw();
        accountRepository.save(account);
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(Long accountId, ChangePasswordRequest request) {
        // 계정 조회
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 새 비밀번호 암호화 후 업데이트
        account.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    // 내 정보 조회
    @Transactional(readOnly = true)
    public MeResponse getCurrentUser() {
        Long accountId = SecurityUtil.getCurrentAccountId();
        Account account = accountRepository.findByIdAndDeletedAtIsNull(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        String storeName = null;
        if (account.getRole() == Role.MANAGER) {
            storeName = managerRepository.findByAccount_Id(accountId)
                    .flatMap(manager -> branchRepository.findByManagerId(manager.getId()))
                    .map(Branch::getStoreName)
                    .orElse(null);
        }
        return MeResponse.from(account, storeName);
    }

    // 이메일 중복 확인
    public void checkEmail(String email) {
        if (accountRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    // 닉네임 중복 확인
    public void checkNickname(String nickname) {
        if (accountRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }
}
