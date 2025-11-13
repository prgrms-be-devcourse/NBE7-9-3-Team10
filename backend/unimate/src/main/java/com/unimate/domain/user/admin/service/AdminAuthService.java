package com.unimate.domain.user.admin.service;

import com.unimate.domain.user.admin.dto.AdminLoginRequest;
import com.unimate.domain.user.admin.dto.AdminSignupRequest;
import com.unimate.domain.user.admin.dto.AdminSignupResponse;
import com.unimate.domain.user.admin.entity.AdminUser;
import com.unimate.domain.user.admin.repository.AdminRepository;
import com.unimate.global.auth.dto.Tokens;
import com.unimate.global.auth.model.SubjectType;
import com.unimate.global.auth.service.TokenService;
import com.unimate.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminUserRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public AdminSignupResponse signup(AdminSignupRequest req) {
        if (adminUserRepository.existsByEmail(req.getEmail())) {
            throw ServiceException.badRequest("이미 등록된 관리자 이메일입니다.");
        }

        AdminUser admin = new AdminUser(
                req.getEmail(),
                passwordEncoder.encode(req.getPassword()),
                req.getName()
        );

        AdminUser savedAdmin = adminUserRepository.save(admin);
        return new AdminSignupResponse(savedAdmin.getId(), savedAdmin.getEmail(), savedAdmin.getName());
    }

    @Transactional
    public Tokens login(AdminLoginRequest req) {
        AdminUser admin = adminUserRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> ServiceException.notFound("관리자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(req.getPassword(), admin.getPassword())) {
            throw ServiceException.unauthorized("비밀번호가 일치하지 않습니다.");
        }

        return tokenService.issueTokens(SubjectType.ADMIN, admin.getId(), admin.getEmail());
    }

    @Transactional(readOnly = true)
    public String reissueAccessToken(String refreshToken) {
        return tokenService.reissueAccessToken(refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.logout(refreshToken);
    }
}
