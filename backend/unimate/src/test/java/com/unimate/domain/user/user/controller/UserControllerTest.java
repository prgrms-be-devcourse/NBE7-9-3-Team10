package com.unimate.domain.user.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimate.domain.user.user.dto.UserUpdateEmailRequest;
import com.unimate.domain.user.user.dto.UserUpdateNameRequest;
import com.unimate.domain.user.user.entity.Gender;
import com.unimate.domain.user.user.entity.User;
import com.unimate.domain.user.user.repository.UserRepository;
import com.unimate.domain.verification.entity.Verification;
import com.unimate.domain.verification.repository.VerificationRepository;
import com.unimate.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private String accessToken;
    private final String baseUrl = "/api/v1/user";
    private final String testEmail = "testuser@university.ac.kr";

    @BeforeEach
    void setup() {
        User user = new User(
                "홍길동",
                testEmail,
                passwordEncoder.encode("password123!"),
                Gender.MALE,
                LocalDate.of(2000, 1, 1),
                "고려대학교"
        );
        userRepository.save(user);

        accessToken = jwtProvider.generateToken(testEmail, user.getId()).getAccessToken();
    }

    @Test
    @DisplayName("GET /api/v1/user - 사용자 정보 조회 성공")
    void getUserInfo_success() throws Exception {
        mockMvc.perform(get(baseUrl)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.university").value("고려대학교"))
                .andExpect(jsonPath("$.gender").value("MALE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/user/name - 사용자 이름 수정 성공")
    void updateUserName_success() throws Exception {
        UserUpdateNameRequest request = new UserUpdateNameRequest("새로운이름");

        mockMvc.perform(patch(baseUrl + "/name")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("새로운이름"));

        User updated = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(updated.getName()).isEqualTo("새로운이름");
    }

    @Test
    @DisplayName("PATCH /api/v1/user/email - 이메일 수정 성공 (인증 완료된 경우)")
    void updateUserEmail_success() throws Exception {
        String newEmail = "newtest@uni.ac.kr";
        String code = "123456";

        Verification verification = new Verification(newEmail, code, LocalDateTime.now().plusMinutes(5));
        verification.markVerified();
        verificationRepository.save(verification);

        UserUpdateEmailRequest request = new UserUpdateEmailRequest(newEmail, code);

        mockMvc.perform(patch(baseUrl + "/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));

        User updated = userRepository.findByEmail(newEmail).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo(newEmail);
    }

    @Test
    @DisplayName("PATCH /api/v1/user/email - 인증되지 않은 이메일로 수정 시 실패 (400)")
    void updateUserEmail_fail_unverified() throws Exception {
        String newEmail = "failtest@uni.ac.kr";

        Verification verification = new Verification(newEmail, "000000", LocalDateTime.now().plusMinutes(5));
        verificationRepository.save(verification);

        UserUpdateEmailRequest request = new UserUpdateEmailRequest(newEmail, "000000");

        mockMvc.perform(patch(baseUrl + "/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되지 않았습니다."));
    }
}