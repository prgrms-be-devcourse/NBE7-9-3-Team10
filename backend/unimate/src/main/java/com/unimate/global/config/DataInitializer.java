//package com.unimate.global.config;
//
// import com.unimate.domain.chatroom.entity.Chatroom;
// import com.unimate.domain.chatroom.repository.ChatroomRepository;
// import com.unimate.domain.match.entity.Match;
// import com.unimate.domain.match.entity.MatchStatus;
// import com.unimate.domain.match.entity.MatchType;
// import com.unimate.domain.match.repository.MatchRepository;
// import com.unimate.domain.message.entity.Message;
// import com.unimate.domain.message.repository.MessageRepository;
// import com.unimate.domain.user.user.entity.Gender;
// import com.unimate.domain.user.user.entity.User;
// import com.unimate.domain.user.user.repository.UserRepository;
// import com.unimate.domain.userMatchPreference.entity.UserMatchPreference;
// import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository;
// import com.unimate.domain.userProfile.entity.UserProfile;
// import com.unimate.domain.userProfile.repository.UserProfileRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional;
//
// import java.math.BigDecimal;
// import java.time.LocalDate;
//
// @Component
// @RequiredArgsConstructor
// public class DataInitializer implements CommandLineRunner {
//
//     private final UserRepository userRepository;
//     private final UserProfileRepository userProfileRepository;
//     private final UserMatchPreferenceRepository userMatchPreferenceRepository;
//     private final MatchRepository matchRepository;
//     private final ChatroomRepository chatroomRepository;
//     private final MessageRepository messageRepository;
//     private final BCryptPasswordEncoder passwordEncoder;
//
//     @Override
//     @Transactional
//     public void run(String... args) throws Exception {
//         // 이미 데이터가 있으면 초기화하지 않음
//         if (userRepository.count() > 0) {
//             return;
//         }
//
//         System.out.println("🌱 시드 데이터 생성 시작...");
//
//         // 사용자들 생성 (비밀번호 암호화) - 순서대로 1~10
//         User user1 = createUser("김서연", "kim@uni.ac.kr", "password123", Gender.FEMALE, LocalDate.of(2002, 3, 15), "서울대학교");
//         User user2 = createUser("이지은", "lee@uni.ac.kr", "password123", Gender.FEMALE, LocalDate.of(2001, 7, 22), "서울대학교");
//         User user3 = createUser("최민수", "choi@uni.ac.kr", "password123", Gender.MALE, LocalDate.of(1999, 11, 8), "서울대학교");
//         User user4 = createUser("정수아", "jung@uni.ac.kr", "password123", Gender.FEMALE, LocalDate.of(2003, 5, 30), "서울대학교");
//         User user5 = createUser("박지민", "park@uni.ac.kr", "password123", Gender.FEMALE, LocalDate.of(2001, 9, 12), "서울대학교");
//         User user6 = createUser("김현우", "kim2@uni.ac.kr", "password123", Gender.MALE, LocalDate.of(2000, 12, 3), "서울대학교");
//         User user7 = createUser("테스트유저", "test@uni.ac.kr", "password123", Gender.MALE, LocalDate.of(1995, 6, 15), "서울대학교");
//         User user8 = createUser("최유진", "yujin@uni.ac.kr", "password123", Gender.FEMALE, LocalDate.of(2002, 8, 20), "서울대학교");
//         User user9 = createUser("이동혁", "donghyuk@uni.ac.kr", "password123", Gender.MALE, LocalDate.of(2001, 4, 10), "서울대학교");
//         User user10 = createUser("박준호", "junho@uni.ac.kr", "password123", Gender.MALE, LocalDate.of(2002, 1, 25), "서울대학교");
//
//         // 프로필들 생성 (UserProfile 엔티티 구조에 맞춤) - 순서대로 1~10
//         // createProfile(user, sleepTime, cleaningFrequency, isSmoker, isPetAllowed, isSnoring, startDate, endDate, mbti)
//         UserProfile profile1 = createProfile(user1, 4, 5, false, false, false, "2025-03-01", "2025-10-30", "INTJ");  // 22시~00시, 매일
//         UserProfile profile2 = createProfile(user2, 5, 4, false, true, false, "2025-02-01", "2025-10-30", "ISTJ");   // 22시 이전, 주 2~3회
//         UserProfile profile3 = createProfile(user3, 2, 2, false, true, true, "2025-01-15", "2025-12-31", "INTP");    // 02시~04시, 월 1~2회
//         UserProfile profile4 = createProfile(user4, 3, 3, false, true, false, "2025-04-01", "2025-11-30", "ESFJ");   // 00시~02시, 주 1회
//         UserProfile profile5 = createProfile(user5, 4, 4, false, false, false, "2025-03-15", "2025-09-15", "ENFJ");  // 22시~00시, 주 2~3회
//         UserProfile profile6 = createProfile(user6, 2, 3, true, true, true, "2025-01-01", "2025-12-31", "ISTP");     // 02시~04시, 주 1회, 흡연
//         UserProfile profile7 = createProfile(user7, 3, 4, false, false, false, "2025-01-01", "2025-12-31", "ESTJ");  // 00시~02시, 주 2~3회
//         UserProfile profile8 = createProfile(user8, 3, 3, false, true, false, "2025-02-15", "2025-11-15", "INFP");   // 00시~02시, 주 1회
//         UserProfile profile9 = createProfile(user9, 4, 5, false, false, false, "2025-03-01", "2025-10-31", "ENTP");  // 22시~00시, 매일
//         UserProfile profile10 = createProfile(user10, 1, 1, true, true, false, "2025-02-01", "2025-12-15", "ENTJ");  // 04시 이후, 거의 안함, 흡연
//
//         // 매칭 선호도 생성 (UserMatchPreference 엔티티 구조에 맞춤)
//         // createMatchPreference(user, sleepTime, cleaningFrequency, hygieneLevel, noiseSensitivity, guestFrequency, drinkingFrequency, startDate, endDate)
//         createMatchPreference(user1, 2, 4, 3, 3, 3, 2, "2025-03-01", "2025-10-30");
//         createMatchPreference(user2, 1, 5, 3, 3, 3, 2, "2025-02-01", "2025-10-30");
//         createMatchPreference(user3, 3, 2, 3, 3, 3, 2, "2025-01-15", "2025-12-31");
//         createMatchPreference(user4, 3, 4, 3, 3, 3, 2, "2025-04-01", "2025-11-30");
//         createMatchPreference(user5, 2, 4, 3, 3, 3, 2, "2025-03-15", "2025-09-15");
//         createMatchPreference(user6, 2, 3, 3, 3, 3, 2, "2025-01-01", "2025-12-31");
//         createMatchPreference(user7, 1, 4, 3, 3, 3, 2, "2025-01-01", "2025-12-31");
//         createMatchPreference(user8, 1, 3, 3, 3, 3, 2, "2025-02-15", "2025-11-15");
//         createMatchPreference(user9, 2, 5, 3, 3, 3, 2, "2025-03-01", "2025-10-31");
//         createMatchPreference(user10, 2, 1, 3, 3, 3, 2, "2025-02-01", "2025-12-15");
//
//         // 매칭 데이터 생성 (다양한 상태 테스트)
//         // 김서연 → 이지은 (REQUEST + ACCEPTED) - 최종 확정된 매칭
//         Match match1 = createMatch(user1, user2, MatchType.REQUEST, MatchStatus.ACCEPTED, new BigDecimal("0.95"));
//         // 최민수 → 정수아 (REQUEST + PENDING) - 대화 후 최종 수락/거절 대기중
//         Match match2 = createMatch(user3, user4, MatchType.REQUEST, MatchStatus.PENDING, new BigDecimal("0.78"));
//         // 테스트유저 → 박지민 (REQUEST + PENDING) - 대화 후 최종 수락/거절 대기중
//         Match match3 = createMatch(user7, user5, MatchType.REQUEST, MatchStatus.PENDING, new BigDecimal("0.82"));
//
//         // 채팅방 생성 (REQUEST 상태의 모든 매칭에 대해 채팅방 생성)
//         Chatroom chatroom1 = createChatroom(user1.getId(), user2.getId()); // ACCEPTED
//         Chatroom chatroom2 = createChatroom(user3.getId(), user4.getId()); // PENDING
//         Chatroom chatroom3 = createChatroom(user7.getId(), user5.getId()); // PENDING
//
//         // 메시지 생성
//         // Chatroom 1 (user1 ↔ user2) - ACCEPTED 상태
//         createMessage(chatroom1.getId(), user1.getId(), "안녕하세요! 룸메이트가 되어서 기뻐요 😊");
//         createMessage(chatroom1.getId(), user2.getId(), "안녕하세요! 저도 기뻐요. 거주 기간이 비슷해서 좋네요");
//         createMessage(chatroom1.getId(), user1.getId(), "네, 맞아요! 생활 패턴도 비슷할 것 같아서 기대돼요");
//
//         // Chatroom 2 (user3 ↔ user4) - PENDING 상태 (최종 수락/거절 대기)
//         createMessage(chatroom2.getId(), user3.getId(), "안녕하세요! 매칭되어서 반갑습니다");
//         createMessage(chatroom2.getId(), user4.getId(), "안녕하세요~ 프로필 봤는데 생활 패턴이 잘 맞을 것 같네요");
//         createMessage(chatroom2.getId(), user3.getId(), "저도 그렇게 생각해요. 청소 빈도나 취침 시간이 비슷하더라구요");
//         createMessage(chatroom2.getId(), user4.getId(), "네! 혹시 소음에 대해서는 어떻게 생각하시나요?");
//         createMessage(chatroom2.getId(), user3.getId(), "저는 조용한 편을 선호해요. 야간에는 특히 조용히 지내려고 노력합니다");
//         createMessage(chatroom2.getId(), user4.getId(), "좋아요! 저도 비슷해요. 그럼 매칭 확정하시겠어요?");
//
//         // Chatroom 3 (user7 ↔ user5) - PENDING 상태 (최종 수락/거절 대기)
//         createMessage(chatroom3.getId(), user7.getId(), "안녕하세요! 룸메이트 찾고 계시죠?");
//         createMessage(chatroom3.getId(), user5.getId(), "네! 반갑습니다. 언제부터 거주 가능하신가요?");
//         createMessage(chatroom3.getId(), user7.getId(), "3월 초부터 가능합니다. 기간도 비슷하게 맞출 수 있을 것 같아요");
//         createMessage(chatroom3.getId(), user5.getId(), "좋네요! 한 가지 더 여쭤봐도 될까요? 반려동물은 어떻게 생각하시나요?");
//         createMessage(chatroom3.getId(), user7.getId(), "저는 반려동물 괜찮아요. 혹시 키우시나요?");
//         createMessage(chatroom3.getId(), user5.getId(), "아니요, 저는 안 키우지만 알레르기가 있어서 물어봤어요");
//         createMessage(chatroom3.getId(), user7.getId(), "아 그렇군요. 저도 키우지 않으니 걱정 안 하셔도 될 것 같아요!");
//
//         System.out.println("✅ 시드 데이터 생성 완료!");
//         System.out.println("📊 생성된 데이터:");
//         System.out.println("   - 사용자: " + userRepository.count() + "명");
//         System.out.println("   - 프로필: " + userProfileRepository.count() + "개");
//         System.out.println("   - 매칭 선호도: " + userMatchPreferenceRepository.count() + "개");
//         System.out.println("   - 매칭: " + matchRepository.count() + "개");
//         System.out.println("   - 채팅방: " + chatroomRepository.count() + "개");
//         System.out.println("   - 메시지: " + messageRepository.count() + "개");
//     }
//
//     private User createUser(String name, String email, String password, Gender gender, LocalDate birthDate, String university) {
//         User user = new User(name, email, passwordEncoder.encode(password), gender, birthDate, university);
//         user.verifyStudent(); // 학생 인증
//         return userRepository.save(user);
//     }
//
//     private UserProfile createProfile(User user, int sleepTime, int cleaningFrequency, boolean isSmoker, boolean isPetAllowed, boolean isSnoring, String startDate, String endDate, String mbti) {
//         // 다양한 값으로 프로필 생성
//         int hygieneLevel = (cleaningFrequency >= 4) ? 4 : (cleaningFrequency <= 2) ? 2 : 3;
//         int noiseSensitivity = (sleepTime >= 4) ? 4 : (sleepTime <= 2) ? 2 : 3;
//         int drinkingFrequency = isSmoker ? 3 : 2; // 흡연자는 음주 빈도가 높을 가능성
//         int guestFrequency = isPetAllowed ? 4 : 3; // 반려동물 허용하는 사람은 손님 초대도 관대
//
//         UserProfile profile = new UserProfile(
//                 user,
//                 sleepTime,
//                 isPetAllowed,
//                 isSmoker,
//                 cleaningFrequency,
//                 5, // preferredAgeGap
//                 hygieneLevel,
//                 isSnoring,
//                 drinkingFrequency,
//                 noiseSensitivity,
//                 guestFrequency,
//                 mbti,
//                 LocalDate.parse(startDate),
//                 LocalDate.parse(endDate),
//                 true
//         );
//         return userProfileRepository.save(profile);
//     }
//
//     private void createMatchPreference(User user, int sleepTime, int cleaningFrequency, int hygieneLevel, int noiseSensitivity, int guestFrequency, int drinkingFrequency, String startDate, String endDate) {
//         UserMatchPreference preference = UserMatchPreference.builder()
//                 .user(user)
//                 .sleepTime(sleepTime)
//                 .cleaningFrequency(cleaningFrequency)
//                 .hygieneLevel(hygieneLevel)
//                 .noiseSensitivity(noiseSensitivity)
//                 .guestFrequency(guestFrequency)
//                 .drinkingFrequency(drinkingFrequency)
//                 .preferredAgeGap(5) // 기본값
//                 .isPetAllowed(true) // 기본값
//                 .isSmoker(false) // 기본값
//                 .isSnoring(false) // 기본값
//                 .startUseDate(LocalDate.parse(startDate))
//                 .endUseDate(LocalDate.parse(endDate))
//                 .build();
//         userMatchPreferenceRepository.save(preference);
//     }
//
//     private Match createMatch(User sender, User receiver, MatchType matchType, MatchStatus matchStatus, BigDecimal preferenceScore) {
//         Match match = Match.builder()
//                 .sender(sender)
//                 .receiver(receiver)
//                 .matchType(matchType)
//                 .matchStatus(matchStatus)
//                 .preferenceScore(preferenceScore)
//                 .build();
//
//         // ACCEPTED 상태인 경우 confirmedAt 설정 (확정 시점 시뮬레이션)
//         if (matchStatus == MatchStatus.ACCEPTED) {
//             match.setConfirmedAt(java.time.LocalDateTime.now().minusDays(1)); // 1일 전 확정으로 설정
//         }
//
//         return matchRepository.save(match);
//     }
//
//     private Chatroom createChatroom(Long user1Id, Long user2Id) {
//         Chatroom chatroom = Chatroom.create(user1Id, user2Id);
//         return chatroomRepository.save(chatroom);
//     }
//
//     private int messageCounter = 0; // 메시지 고유 ID 생성용 카운터
//
//     private void createMessage(Long chatroomId, Long senderId, String content) {
//         Chatroom chatroom = chatroomRepository.findById(chatroomId).orElseThrow();
//         Message message = Message.builder()
//                 .chatroom(chatroom)
//                 .senderId(senderId)
//                 .content(content)
//                 .clientMessageId("seed-" + System.currentTimeMillis() + "-" + senderId + "-" + (++messageCounter))
//                 .build();
//         messageRepository.save(message);
//     }
// }
