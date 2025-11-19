package com.unimate.global.config

import com.unimate.domain.chatroom.entity.Chatroom
import com.unimate.domain.chatroom.repository.ChatroomRepository
import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.message.entity.Message
import com.unimate.domain.message.repository.MessageRepository
import com.unimate.domain.review.entity.Review
import com.unimate.domain.review.repository.ReviewRepository
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.domain.userProfile.entity.UserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userMatchPreferenceRepository: UserMatchPreferenceRepository,
    private val matchRepository: MatchRepository,
    private val chatroomRepository: ChatroomRepository,
    private val messageRepository: MessageRepository,
    private val reviewRepository: ReviewRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) : CommandLineRunner {

    companion object {
        private const val TEST_PASSWORD = "password123!"
        private const val TEST_UNIVERSITY = "서울대학교"
    }

    @Transactional
    override fun run(vararg args: String) {
        // 이미 데이터가 있으면 초기화하지 않음
        if (userRepository.count() > 0) {
            return
        }

        println("🌱 시드 데이터 생성 시작...")

        // 사용자들 생성 (비밀번호 암호화) - 순서대로 1~10
        val user1 = createUser("김서연", "kim@snu.ac.kr", TEST_PASSWORD, Gender.FEMALE, LocalDate.of(2002, 3, 15), TEST_UNIVERSITY)
        val user2 = createUser("이지은", "lee@snu.ac.kr", TEST_PASSWORD, Gender.FEMALE, LocalDate.of(2001, 7, 22), TEST_UNIVERSITY)
        val user3 = createUser("최민수", "choi@snu.ac.kr", TEST_PASSWORD, Gender.MALE, LocalDate.of(1999, 11, 8), TEST_UNIVERSITY)
        val user4 = createUser("정수아", "jung@snu.ac.kr", TEST_PASSWORD, Gender.FEMALE, LocalDate.of(2003, 5, 30), TEST_UNIVERSITY)
        val user5 = createUser("박지민", "park@snu.ac.kr", TEST_PASSWORD, Gender.FEMALE, LocalDate.of(2001, 9, 12), TEST_UNIVERSITY)
        val user6 = createUser("김현우", "kim2@snu.ac.kr", TEST_PASSWORD, Gender.MALE, LocalDate.of(2000, 12, 3), TEST_UNIVERSITY)
        val user7 = createUser("테스트유저", "testuser@snu.ac.kr", TEST_PASSWORD, Gender.MALE, LocalDate.of(1995, 6, 15), TEST_UNIVERSITY)
        val user8 = createUser("최유진", "yujin@snu.ac.kr", TEST_PASSWORD, Gender.FEMALE, LocalDate.of(2002, 8, 20), TEST_UNIVERSITY)
        val user9 = createUser("이동혁", "donghyuk@snu.ac.kr", TEST_PASSWORD, Gender.MALE, LocalDate.of(2001, 4, 10), TEST_UNIVERSITY)
        val user10 = createUser("박준호", "junho@snu.ac.kr", TEST_PASSWORD, Gender.MALE, LocalDate.of(2002, 1, 25), TEST_UNIVERSITY)

        // 프로필들 생성 (UserProfile 엔티티 구조에 맞춤) - 순서대로 1~10
        // createProfile(user, sleepTime, cleaningFrequency, isSmoker, isPetAllowed, isSnoring, startDate, endDate, mbti)
        // 리뷰 테스트를 위해 일부 프로필의 endDate를 과거로 설정
        val profile1 = createProfile(user1, 4, 5, false, false, false, "2024-03-01", "2024-10-30", "INTJ") // 종료됨 (리뷰 작성 가능)
        val profile2 = createProfile(user2, 5, 4, false, true, false, "2024-02-01", "2024-10-30", "ISTJ") // 종료됨 (리뷰 작성 가능)
        val profile3 = createProfile(user3, 2, 2, false, true, true, "2025-01-15", "2025-12-31", "INTP") // 진행 중
        val profile4 = createProfile(user4, 3, 3, false, true, false, "2024-04-01", "2024-11-30", "ESFJ") // 종료됨 (리뷰 작성 가능) 
        val profile5 = createProfile(user5, 4, 4, false, false, false, "2024-03-15", "2024-09-15", "ENFJ") // 종료됨 (리뷰 작성 가능)
        val profile6 = createProfile(user6, 2, 3, true, true, true, "2025-01-01", "2025-12-31", "ISTP") // 진행 중
        val profile7 = createProfile(user7, 3, 4, false, false, false, "2024-01-01", "2024-12-31", "ESTJ") // 종료됨 (리뷰 작성 가능)
        val profile8 = createProfile(user8, 3, 3, false, true, false, "2024-02-15", "2024-11-15", "INFP") // 종료됨 (리뷰 작성 가능) 
        val profile9 = createProfile(user9, 4, 5, false, false, false, "2024-03-01", "2024-10-31", "ENTP") // 종료됨 (리뷰 작성 가능)
        val profile10 = createProfile(user10, 1, 1, true, true, false, "2025-02-01", "2025-12-15", "ENTJ") // 진행 중

        // 매칭 선호도 생성 (UserMatchPreference 엔티티 구조에 맞춤)
        // createMatchPreference(user, sleepTime, cleaningFrequency, hygieneLevel, noiseSensitivity, guestFrequency, drinkingFrequency, startDate, endDate)
        createMatchPreference(user1, 2, 4, 3, 3, 3, 2, "2024-03-01", "2024-10-30")
        createMatchPreference(user2, 1, 5, 3, 3, 3, 2, "2024-02-01", "2024-10-30")
        createMatchPreference(user3, 3, 2, 3, 3, 3, 2, "2025-01-15", "2025-12-31")
        createMatchPreference(user4, 3, 4, 3, 3, 3, 2, "2025-04-01", "2025-11-30")
        createMatchPreference(user5, 2, 4, 3, 3, 3, 2, "2024-03-15", "2024-09-15")
        createMatchPreference(user6, 2, 3, 3, 3, 3, 2, "2025-01-01", "2025-12-31")
        createMatchPreference(user7, 1, 4, 3, 3, 3, 2, "2024-01-01", "2024-12-31")
        createMatchPreference(user8, 1, 3, 3, 3, 3, 2, "2025-02-15", "2025-11-15")
        createMatchPreference(user9, 2, 5, 3, 3, 3, 2, "2024-03-01", "2024-10-31")
        createMatchPreference(user10, 2, 1, 3, 3, 3, 2, "2025-02-01", "2025-12-15")

        // 매칭 데이터 생성 (다양한 상태 테스트)
        // 김서연 → 이지은 (REQUEST + ACCEPTED) - 100일 전 확정, 종료됨 (리뷰 작성 가능)
        val match1 = createMatch(user1, user2, MatchType.REQUEST, MatchStatus.ACCEPTED, BigDecimal("0.95"), 100)
        // 최민수 → 정수아 (REQUEST + PENDING) - 대화 후 최종 수락/거절 대기중
        val match2 = createMatch(user3, user4, MatchType.REQUEST, MatchStatus.PENDING, BigDecimal("0.78"), 1)
        // 테스트유저 → 박지민 (REQUEST + PENDING) - 대화 후 최종 수락/거절 대기중
        val match3 = createMatch(user7, user5, MatchType.REQUEST, MatchStatus.PENDING, BigDecimal("0.82"), 1)
        // 테스트유저 → 박준호 (REQUEST + ACCEPTED) - 100일 전 확정, 종료됨 (리뷰 작성 가능)
        val match4 = createMatch(user7, user10, MatchType.REQUEST, MatchStatus.ACCEPTED, BigDecimal("0.88"), 100)
        // 김현우 → 최유진 (REQUEST + ACCEPTED) - 100일 전 확정, 종료됨 (리뷰 작성 가능)
        val match5 = createMatch(user6, user8, MatchType.REQUEST, MatchStatus.ACCEPTED, BigDecimal("0.75"), 100)
        // 이동혁 → 박준호 (REQUEST + ACCEPTED) - 50일 전 확정, 종료됨 (리뷰 작성 불가 - 90일 미만)
        val match6 = createMatch(user9, user10, MatchType.REQUEST, MatchStatus.ACCEPTED, BigDecimal("0.80"), 50)
        // ✅ 추가: 리뷰 작성 가능한 매칭 (양방향 모두 리뷰 미작성)
        // 김서연 → 정수아 (REQUEST + ACCEPTED) - 100일 전 확정, 종료됨 (리뷰 작성 가능)
        val match7 = createMatch(user1, user4, MatchType.REQUEST, MatchStatus.ACCEPTED, BigDecimal("0.85"), 100)
        // 박지민 → 최유진 (REQUEST + ACCEPTED) - 100일 전 확정, 종료됨 (리뷰 작성 가능)
        val match8 = createMatch(user5, user8, MatchType.REQUEST, MatchStatus.ACCEPTED, BigDecimal("0.90"), 100)

        // 채팅방 생성 (REQUEST 상태의 모든 매칭에 대해 채팅방 생성)
        val chatroom1 = createChatroom(user1.id, user2.id) // ACCEPTED
        val chatroom2 = createChatroom(user3.id, user4.id) // PENDING
        val chatroom3 = createChatroom(user7.id, user5.id) // PENDING
        val chatroom4 = createChatroom(user7.id, user10.id) // ACCEPTED
        val chatroom5 = createChatroom(user6.id, user8.id) // ACCEPTED
        val chatroom6 = createChatroom(user9.id, user10.id) // ACCEPTED
        val chatroom7 = createChatroom(user1.id, user4.id) // ACCEPTED
        val chatroom8 = createChatroom(user5.id, user8.id) // ACCEPTED

        // 메시지 생성
        // Chatroom 1 (user1 ↔ user2) - ACCEPTED 상태
        createMessage(chatroom1.id!!, user1.id, "안녕하세요! 룸메이트가 되어서 기뻐요 😊")
        createMessage(chatroom1.id!!, user2.id, "안녕하세요! 저도 기뻐요. 거주 기간이 비슷해서 좋네요")
        createMessage(chatroom1.id!!, user1.id, "네, 맞아요! 생활 패턴도 비슷할 것 같아서 기대돼요")

        // Chatroom 2 (user3 ↔ user4) - PENDING 상태 (최종 수락/거절 대기)
        createMessage(chatroom2.id!!, user3.id, "안녕하세요! 매칭되어서 반갑습니다")
        createMessage(chatroom2.id!!, user4.id, "안녕하세요~ 프로필 봤는데 생활 패턴이 잘 맞을 것 같네요")
        createMessage(chatroom2.id!!, user3.id, "저도 그렇게 생각해요. 청소 빈도나 취침 시간이 비슷하더라구요")
        createMessage(chatroom2.id!!, user4.id, "네! 혹시 소음에 대해서는 어떻게 생각하시나요?")
        createMessage(chatroom2.id!!, user3.id, "저는 조용한 편을 선호해요. 야간에는 특히 조용히 지내려고 노력합니다")
        createMessage(chatroom2.id!!, user4.id, "좋아요! 저도 비슷해요. 그럼 매칭 확정하시겠어요?")

        // Chatroom 3 (user7 ↔ user5) - PENDING 상태 (최종 수락/거절 대기)
        createMessage(chatroom3.id!!, user7.id, "안녕하세요! 룸메이트 찾고 계시죠?")
        createMessage(chatroom3.id!!, user5.id, "네! 반갑습니다. 언제부터 거주 가능하신가요?")
        createMessage(chatroom3.id!!, user7.id, "3월 초부터 가능합니다. 기간도 비슷하게 맞출 수 있을 것 같아요")
        createMessage(chatroom3.id!!, user5.id, "좋네요! 한 가지 더 여쭤봐도 될까요? 반려동물은 어떻게 생각하시나요?")
        createMessage(chatroom3.id!!, user7.id, "저는 반려동물 괜찮아요. 혹시 키우시나요?")
        createMessage(chatroom3.id!!, user5.id, "아니요, 저는 안 키우지만 알레르기가 있어서 물어봤어요")
        createMessage(chatroom3.id!!, user7.id, "아 그렇군요. 저도 키우지 않으니 걱정 안 하셔도 될 것 같아요!")

        // 리뷰 데이터 생성
        // match1: user1(김서연) → user2(이지은) - 양방향 리뷰 작성 (둘 다 추천)
        createReview(match1, user1, user2, 5, "정말 좋은 룸메이트였어요! 생활 패턴도 잘 맞고 깔끔하게 생활하시는 분이에요.", true)
        createReview(match1, user2, user1, 5, "서연님과 함께 생활하면서 정말 편안했어요. 다음에도 함께하고 싶어요!", true)

        // match4: user7(테스트유저) → user10(박준호) - 한쪽만 리뷰 작성 (추천)
        createReview(match4, user7, user10, 4, "준호님은 시간 약속을 잘 지키시고 생활 패턴도 규칙적이셨어요.", true)

        // match5: user6(김현우) → user8(최유진) - 양방향 리뷰 작성 (한쪽은 추천, 한쪽은 비추천)
        createReview(match5, user6, user8, 3, "생활 패턴이 조금 달라서 적응하는데 시간이 걸렸어요.", false)
        createReview(match5, user8, user6, 4, "현우님은 친절하시지만 흡연하시는 부분이 조금 아쉬웠어요.", false)

        // match7, match8은 리뷰를 작성하지 않음 (프론트엔드에서 테스트용)

        println("✅ 시드 데이터 생성 완료!")
        println("📊 생성된 데이터:")
        println("   - 사용자: ${userRepository.count()}명")
        println("   - 프로필: ${userProfileRepository.count()}개")
        println("   - 매칭 선호도: ${userMatchPreferenceRepository.count()}개")
        println("   - 매칭: ${matchRepository.count()}개")
        println("   - 채팅방: ${chatroomRepository.count()}개")
        println("   - 메시지: ${messageRepository.count()}개")
        println("   - 리뷰: ${reviewRepository.count()}개")
    }

    private fun createUser(
        name: String,
        email: String,
        password: String,
        gender: Gender,
        birthDate: LocalDate,
        university: String
    ): User {
        val user = User(
            name = name,
            email = email,
            password = passwordEncoder.encode(password),
            gender = gender,
            birthDate = birthDate,
            university = university
        )
        user.studentVerified = true
        return userRepository.save(user)
    }

    private fun createProfile(
        user: User,
        sleepTime: Int,
        cleaningFrequency: Int,
        isSmoker: Boolean,
        isPetAllowed: Boolean,
        isSnoring: Boolean,
        startDate: String,
        endDate: String,
        mbti: String
    ): UserProfile {
        // 다양한 값으로 프로필 생성
        val hygieneLevel = if (cleaningFrequency >= 4) 4 else if (cleaningFrequency <= 2) 2 else 3
        val noiseSensitivity = if (sleepTime >= 4) 4 else if (sleepTime <= 2) 2 else 3
        val drinkingFrequency = if (isSmoker) 3 else 2 // 흡연자는 음주 빈도가 높을 가능성
        val guestFrequency = if (isPetAllowed) 4 else 3 // 반려동물 허용하는 사람은 손님 초대도 관대

        val profile = UserProfile(
            user = user,
            sleepTime = sleepTime,
            isPetAllowed = isPetAllowed,
            isSmoker = isSmoker,
            cleaningFrequency = cleaningFrequency,
            preferredAgeGap = 5,
            hygieneLevel = hygieneLevel,
            isSnoring = isSnoring,
            drinkingFrequency = drinkingFrequency,
            noiseSensitivity = noiseSensitivity,
            guestFrequency = guestFrequency,
            mbti = mbti,
            startUseDate = LocalDate.parse(startDate),
            endUseDate = LocalDate.parse(endDate),
            matchingEnabled = true
        )
        return userProfileRepository.save(profile)
    }

    private fun createMatchPreference(
        user: User,
        sleepTime: Int,
        cleaningFrequency: Int,
        hygieneLevel: Int,
        noiseSensitivity: Int,
        guestFrequency: Int,
        drinkingFrequency: Int,
        startDate: String,
        endDate: String
    ) {
        val preference = UserMatchPreference(
            user = user,
            startUseDate = LocalDate.parse(startDate),
            endUseDate = LocalDate.parse(endDate),
            sleepTime = sleepTime,
            isPetAllowed = true,
            isSmoker = false,
            cleaningFrequency = cleaningFrequency,
            preferredAgeGap = 5,
            hygieneLevel = hygieneLevel,
            isSnoring = false,
            drinkingFrequency = drinkingFrequency,
            noiseSensitivity = noiseSensitivity,
            guestFrequency = guestFrequency
        )
        userMatchPreferenceRepository.save(preference)
    }

    private fun createMatch(
        sender: User,
        receiver: User,
        matchType: MatchType,
        matchStatus: MatchStatus,
        preferenceScore: BigDecimal,
        daysAgo: Int = 1 // 확정일로부터 며칠 전인지 (기본값 1일)
    ): Match {
        val match = Match.createRequest(
            sender = sender,
            receiver = receiver,
            preferenceScore = preferenceScore
        )

        // 상태 설정
        match.matchStatus = matchStatus
        match.senderResponse = matchStatus
        match.receiverResponse = matchStatus

        // ACCEPTED 상태인 경우 confirmedAt 설정 (확정 시점 시뮬레이션)
        if (matchStatus == MatchStatus.ACCEPTED) {
            match.confirmedAt = LocalDateTime.now().minusDays(daysAgo.toLong())
        }

        return matchRepository.save(match)
    }

    private fun createChatroom(user1Id: Long?, user2Id: Long?): Chatroom {
        val user1 = user1Id ?: throw IllegalStateException("user1Id가 null입니다.")
        val user2 = user2Id ?: throw IllegalStateException("user2Id가 null입니다.")
        val chatroom = Chatroom.create(user1, user2)
        return chatroomRepository.save(chatroom)
    }

    private var messageCounter = 0 // 메시지 고유 ID 생성용 카운터

    private fun createMessage(chatroomId: Long, senderId: Long?, content: String?) {
        val chatroom = chatroomRepository.findById(chatroomId)
            .orElse(null) ?: throw IllegalStateException("채팅방을 찾을 수 없습니다: $chatroomId")

        val sender = senderId ?: throw IllegalStateException("senderId가 null입니다.")
        val messageContent = content ?: throw IllegalStateException("content가 null입니다.")


        val message = Message(
            chatroom = chatroom,
            senderId = sender,
            content = messageContent,
            clientMessageId = "seed-${System.currentTimeMillis()}-$sender-${++messageCounter}"
        )
        messageRepository.save(message)
    }

    private fun createReview(
        match: Match,
        reviewer: User,
        reviewee: User,
        rating: Int,
        content: String?,
        recommend: Boolean
    ) {
        val review = Review(
            match = match,
            reviewer = reviewer,
            reviewee = reviewee,
            rating = rating,
            content = content,
            recommend = recommend,
            canRematch = recommend // 추천 = 재매칭 가능
        )
        reviewRepository.save(review)
    }
}