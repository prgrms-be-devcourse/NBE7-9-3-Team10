package com.unimate.domain.match.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimate.domain.match.dto.MatchConfirmRequest;
import com.unimate.domain.match.entity.Match;
import com.unimate.domain.match.entity.MatchStatus;
import com.unimate.domain.match.repository.MatchRepository;
import com.unimate.domain.user.user.dto.UserLoginRequest;
import com.unimate.domain.user.user.entity.Gender;
import com.unimate.domain.user.user.entity.User;
import com.unimate.domain.user.user.repository.UserRepository;
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference;
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository;
import com.unimate.domain.userProfile.entity.UserProfile;
import com.unimate.domain.userProfile.repository.UserProfileRepository;
import com.unimate.domain.match.service.MatchCacheService;
import com.unimate.global.mail.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MatchControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private UserMatchPreferenceRepository userMatchPreferenceRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private MatchCacheService matchCacheService;
    @MockitoBean private MailSender mailSender;
    @MockitoBean private EmailService emailService;

    private User sender;
    private User receiver;
    private User thirdUser;
    private String senderToken;
    private String receiverToken;

    private final String baseUrl = "/api/v1/matches";

    @BeforeEach
    void setUp() throws Exception {
        matchRepository.deleteAll();
        userMatchPreferenceRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        sender = createUser("sender@test.ac.kr", "송신자", Gender.MALE);
        receiver = createUser("receiver@test.ac.kr", "수신자", Gender.MALE);
        // thirdUser는 동일 대학으로 설정하여 추천 목록에 포함되도록 함
        thirdUser = createUser("third@test.ac.kr", "제삼자", Gender.MALE);

        createUserProfile(sender, true);
        createUserProfile(receiver, true);
        createUserProfile(thirdUser, true);

        createUserPreference(sender);
        createUserPreference(receiver);
        createUserPreference(thirdUser);

        // 캐시 무효화 후 재로딩 (테스트 데이터가 캐시에 반영되도록)
        // 개별 프로필 캐시도 무효화 (getUserProfileById에서 사용)
        matchCacheService.evictUserProfileCache(sender.getId());
        matchCacheService.evictUserProfileCache(receiver.getId());
        matchCacheService.evictUserProfileCache(thirdUser.getId());
        // 전체 후보 캐시도 무효화
        matchCacheService.evictAllCandidatesCache();


        senderToken = login(sender.getEmail(), "password123!");
        receiverToken = login(receiver.getEmail(), "password123!");
    }

    private User createUser(String email, String name, Gender gender) {
        User u = new User(
                name,
                email,
                passwordEncoder.encode("password123!"),
                gender,
                LocalDate.now().minusYears(27),  // 26-28세 범위에 맞추기 위해 27세로 설정
                "서울대학교"
        );
        u.verifyStudent();
        return userRepository.save(u);
    }

    private void createUserProfile(User user, boolean enabled) {
        UserProfile profile = new UserProfile(
                user
                ,3
                ,true
                ,false
                ,3
                ,2
                ,3
                ,false
                ,1
                ,2
                ,1
                ,"INTP"
                ,LocalDate.now()
                ,LocalDate.now()
                ,enabled
        );
        userProfileRepository.save(profile);
    }

    private void createUserPreference(User user) {
        UserMatchPreference pref = new UserMatchPreference(
                user,
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                3,  // sleepTime
                true, // isPetAllowed
                false, // isSmoker
                3,  // cleaningFrequency
                2, // preferredAgeGap
                3, // hygieneLevel
                false, // isSnoring
                1, // drinkingFrequency
                2, // noiseSensitivity
                1 // guestFrequency
        );
        userMatchPreferenceRepository.save(pref);
    }

    private String login(String email, String password) throws Exception {
        UserLoginRequest req = new UserLoginRequest(email, password);

        String json = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ───────────────────────────────────────────────

    @Test
    @DisplayName("추천 목록 조회 성공")
    void getRecommendations_success() throws Exception {
        // receiver와 thirdUser가 추천 목록에 포함되어야 함
        // (sender 자신은 제외, 동일 성별, 동일 대학, 필터 조건 만족)
        mockMvc.perform(
                        get(baseUrl + "/recommendations")
                                .header("Authorization", bearer(senderToken))
                                .param("sleepPattern", "normal")
                                .param("ageRange", "26-28")  // 23-25 → 26-28로 변경
                                .param("cleaningFrequency", "weekly")
                                .param("startDate", LocalDate.now().toString())
                                .param("endDate", LocalDate.now().plusMonths(6).toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations.length()").value(2)); // receiver, thirdUser
    }

    @Test
    @DisplayName("추천 상세 조회 성공 - Match가 있는 경우")
    void getCandidateDetail_success_withMatch() throws Exception {
        // Match 생성
        Match existingMatch = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.8));
        matchRepository.save(existingMatch);

        mockMvc.perform(
                        get(baseUrl + "/candidates/" + receiver.getId())
                                .header("Authorization", bearer(senderToken))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverId").value(receiver.getId()))
                .andExpect(jsonPath("$.matchType").value("REQUEST"))
                .andExpect(jsonPath("$.matchStatus").value("PENDING"))
                .andExpect(jsonPath("$.preferenceScore").exists())
                .andExpect(jsonPath("$.name").value(receiver.getName()))
                .andExpect(jsonPath("$.email").value(receiver.getEmail()))
                .andExpect(jsonPath("$.university").value(receiver.getUniversity()));
    }

    @Test
    @DisplayName("좋아요 취소 성공")
    void cancelLike_success() throws Exception {
        matchRepository.save(Match.createLike(sender, receiver, BigDecimal.valueOf(0.8)));

        mockMvc.perform(
                        delete(baseUrl + "/" + receiver.getId())
                                .header("Authorization", bearer(senderToken))
                )
                .andExpect(status().isNoContent());

        assertThat(matchRepository.findBySenderIdAndReceiverId(sender.getId(), receiver.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("REQUEST 상태 → 한쪽 accept 성공")
    void confirmMatch_oneSideAccept_success() throws Exception {
        Match m = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.85));
        matchRepository.save(m);

        MatchConfirmRequest req = new MatchConfirmRequest("accept");

        mockMvc.perform(
                        put(baseUrl + "/" + m.getId() + "/confirm")
                                .header("Authorization", bearer(senderToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("PENDING"))
                .andExpect(jsonPath("$.message").value("룸메이트 매칭이 최종 확정되었습니다."));

        Match updated = matchRepository.findById(m.getId()).orElseThrow();
        assertThat(updated.getSenderResponse()).isEqualTo(MatchStatus.ACCEPTED);
    }

    @Test
    @DisplayName("양쪽 accept → 최종 ACCEPTED")
    void confirmMatch_bothAccept_success() throws Exception {
        Match m = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.85));
        matchRepository.save(m);

        MatchConfirmRequest req = new MatchConfirmRequest("accept");

        mockMvc.perform(
                        put(baseUrl + "/" + m.getId() + "/confirm")
                                .header("Authorization", bearer(senderToken))
                                .content(objectMapper.writeValueAsString(req))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        put(baseUrl + "/" + m.getId() + "/confirm")
                                .header("Authorization", bearer(receiverToken))
                                .content(objectMapper.writeValueAsString(req))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("ACCEPTED"));
    }

    @Test
    @DisplayName("결과 조회 성공 (ACCEPTED만)")
    void getMatchResults_success() throws Exception {

        Match accepted = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.75));
        accepted.processUserResponse(sender.getId(), MatchStatus.ACCEPTED);
        accepted.processUserResponse(receiver.getId(), MatchStatus.ACCEPTED);
        matchRepository.save(accepted);

        Match pending = Match.createRequest(sender, thirdUser, BigDecimal.valueOf(0.60));
        matchRepository.save(pending);

        mockMvc.perform(
                        get(baseUrl + "/results")
                                .header("Authorization", bearer(senderToken))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].matchStatus").value("ACCEPTED"));
    }

    @Test
    @DisplayName("인증 없이 접근 → 401")
    void unauthorized_fail() throws Exception {
        mockMvc.perform(
                get(baseUrl + "/status")
        ).andExpect(status().isUnauthorized());
    }
}