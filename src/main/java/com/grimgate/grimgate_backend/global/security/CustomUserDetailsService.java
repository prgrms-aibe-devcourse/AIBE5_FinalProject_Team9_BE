package com.grimgate.grimgate_backend.global.security;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

// Spring Security 인증에 사용할 사용자 정보 로드
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    // accountId(String)로 계정을 조회하여 UserDetails 반환 (탈퇴 계정 제외)
    @Override
    public UserDetails loadUserByUsername(String accountId) throws UsernameNotFoundException {
        Long id = Long.parseLong(accountId);

        Account account = accountRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UsernameNotFoundException("계정을 찾을 수 없습니다. accountId: " + accountId));

        return new User(
                String.valueOf(account.getId()),
                account.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
        );
    }
}
