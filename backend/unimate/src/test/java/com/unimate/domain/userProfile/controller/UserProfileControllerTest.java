package com.unimate.domain.userProfile.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimate.domain.user.user.dto.UserLoginRequest;
import com.unimate.domain.user.user.entity.Gender;
import com.unimate.domain.user.user.entity.User;
import com.unimate.domain.user.user.repository.UserRepository;
import com.unimate.domain.userProfile.dto.ProfileCreateRequest;
import com.unimate.domain.userProfile.repository.UserProfileRepository;
import com.unimate.domain.verification.repository.VerificationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class UserProfileControllerTest {

    @Autowired private MockMvc                mvc;
    @Autowired private ObjectMapper           objectMapper;
    @Autowired private UserRepository         userRepository;
    @Autowired private UserProfileRepository  userProfileRepository;
    @Autowired private VerificationRepository verificationRepository;
    @Autowired private BCryptPasswordEncoder  passwordEncoder;

    //---------------------- FIXTURE ------------------------------
    private String email;
    private String rawPassword;

    @BeforeEach
    void setUp() throws Exception {
        email = "tester@test.ac.kr";
        rawPassword = "test1234";

        // 이미 존재하면 중복 생성 방지
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
        User u = new User(
                "테스트유저",
                email,
                passwordEncoder.encode(rawPassword),
                Gender.MALE, // enum으로 변경
                LocalDate.of(1991,1,1),
                "서울대");
        u.verifyStudent();
        userRepository.save(u);

        // 프로필 테이블은 비워두고 시작
        userProfileRepository.deleteAll();
    }

    //---------------------- HELPER -------------------------------
    private String loginAndGetAccessToken() throws Exception {
        String body = """
                {
                    "email":"%s",
                    "password":"%s"
                }
                """.formatted(email, rawPassword);

        UserLoginRequest loginRequest = new UserLoginRequest(email, rawPassword);

        String json = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(json).path("accessToken").asText(null);
        assertThat(token).as("login accessToken null").isNotBlank();
        return token;
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private ProfileCreateRequest sampleCreateReq() {
        return new ProfileCreateRequest(
                1
                ,true
                ,false
                ,2
                ,3
                ,3
                ,true
                ,2
                ,3
                ,1
                ,"INFP"
                ,LocalDate.now()
                ,LocalDate.now()
                ,false
        );
    }

    private ProfileCreateRequest sampleUpdateReq() {
        return new ProfileCreateRequest(
                1
                ,false
                ,false
                ,2
                ,3
                ,1
                ,false
                ,2
                ,4
                ,1
                ,"ENTP"
                ,LocalDate.now()
                ,LocalDate.now()
                ,false
                );
    }

    //---------------------- TEST CODE ----------------------------
    @Test
    @DisplayName("프로필 생성 성공 테스트")
    void t1() throws Exception {
        String token = loginAndGetAccessToken();

        String reqJson = objectMapper.writeValueAsString(sampleCreateReq());
        String resJson = mvc.perform(post("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(resJson);
        Long profileId = node.get("id").asLong();

        // 검증은 id만 확인해보기
        assertThat(profileId).isPositive();
        assertThat(userProfileRepository.findById(profileId)).isPresent();
    }

    @Test
    @DisplayName("프로필 수정 성공 테스트")
    void t2() throws Exception {
        String token = loginAndGetAccessToken();

        // 생성
        String createJson = objectMapper.writeValueAsString(sampleCreateReq());
        String created = mvc.perform(post("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        // 수정
        String updateJson = objectMapper.writeValueAsString(sampleUpdateReq());

        String updated = mvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andReturn().getResponse().getContentAsString();

        // 검증
        JsonNode node = objectMapper.readTree(updated);
        assertThat(node.get("sleepTime"   ).asInt()    ).isEqualTo(1);
        assertThat(node.get("isPetAllowed").asBoolean()).isEqualTo(false);
        assertThat(node.get("mbti"        ).asText()   ).isEqualTo("ENTP");
    }
}
