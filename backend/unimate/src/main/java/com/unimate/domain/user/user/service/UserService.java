package com.unimate.domain.user.user.service;

import com.unimate.domain.match.service.MatchCacheService;
import com.unimate.domain.user.user.dto.UserUpdateEmailRequest;
import com.unimate.domain.user.user.entity.User;
import com.unimate.domain.user.user.repository.UserRepository;
import com.unimate.domain.verification.entity.Verification;
import com.unimate.domain.verification.repository.VerificationRepository;
import com.unimate.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final MatchCacheService matchCacheService;

    @Transactional
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ServiceException.notFound("사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public User updateName(String email, String name){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ServiceException.notFound("사용자를 찾을 수 없습니다."));
        user.updateName(name);
        
        matchCacheService.evictUserProfileCache(user.getId());
        log.info("유저 이름 변경 - 캐시 무효화 (userId: {})", user.getId());
        
        return userRepository.save(user);
    }

    @Transactional
    public User updateEmail(String currentEmail, UserUpdateEmailRequest req) {
        User user = findByEmail(currentEmail);

        Verification verification = verificationRepository.findByEmail(req.getNewEmail())
                .orElseThrow(() -> ServiceException.badRequest("인증 요청이 존재하지 않습니다."));

        if (!verification.isVerified()) {
            throw ServiceException.badRequest("이메일 인증이 완료되지 않았습니다.");
        }

        if (!verification.getCode().equals(req.getCode())) {
            throw ServiceException.unauthorized("인증 코드가 올바르지 않습니다.");
        }

        if (userRepository.existsByEmail(req.getNewEmail())) {
            throw ServiceException.badRequest("이미 등록된 이메일입니다.");
        }

        user.updateEmail(req.getNewEmail());

        verificationRepository.delete(verification);

        return user;
    }
}