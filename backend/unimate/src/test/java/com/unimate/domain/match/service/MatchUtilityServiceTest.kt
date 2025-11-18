package com.unimate.domain.match.service

import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchUtilityServiceTest {

    @Autowired
    private lateinit var matchUtilityService: MatchUtilityService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var matchRepository: MatchRepository

    @BeforeEach
    fun setUp() {
        matchRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("나이 계산 - 생일이 지난 경우")
    fun `calculateAge should return correct age when birthday has passed`() {
        // given
        val birthDate = LocalDate.now().minusYears(25).minusMonths(2)

        // when
        val age = matchUtilityService.calculateAge(birthDate)

        // then
        assertThat(age).isEqualTo(25)
    }

    @Test
    @DisplayName("나이 계산 - 생일이 아직 안 지난 경우")
    fun `calculateAge should return correct age when birthday has not passed`() {
        // given
        val birthDate = LocalDate.now().minusYears(25).plusMonths(2)

        // when
        val age = matchUtilityService.calculateAge(birthDate)

        // then
        assertThat(age).isEqualTo(24)
    }

    @Test
    @DisplayName("나이 계산 - 생일이 지난 경우 (정확한 날짜)")
    fun `calculateAge should return correct age when birthday has passed with exact date`() {
        // given - 생일이 지난 경우 (예: 오늘이 3월 15일이고 생일이 1월 15일)
        val today = LocalDate.now()
        val birthDate = LocalDate.of(today.year - 25, today.month, today.dayOfMonth).minusMonths(2)

        // when
        val age = matchUtilityService.calculateAge(birthDate)

        // then
        assertThat(age).isEqualTo(25)
    }

    @Test
    @DisplayName("나이 계산 - 생일이 아직 안 지난 경우 (정확한 날짜)")
    fun `calculateAge should return correct age when birthday has not passed with exact date`() {
        // given - 생일이 아직 안 지난 경우 (예: 오늘이 1월 15일이고 생일이 3월 15일)
        val today = LocalDate.now()
        val birthDate = LocalDate.of(today.year - 25, today.month, today.dayOfMonth).plusMonths(2)

        // when
        val age = matchUtilityService.calculateAge(birthDate)

        // then
        assertThat(age).isEqualTo(24)
    }

    @Test
    @DisplayName("나이 계산 - 미래 날짜인 경우 0 반환")
    fun `calculateAge should return 0 for future date`() {
        // given
        val birthDate = LocalDate.now().plusYears(1)

        // when
        val age = matchUtilityService.calculateAge(birthDate)

        // then
        assertThat(age).isEqualTo(0)
    }

    @Test
    @DisplayName("상태 메시지 반환 - NONE")
    fun `getStatusMessage should return correct message for NONE`() {
        // when
        val message = matchUtilityService.getStatusMessage(MatchStatus.NONE)

        // then
        assertThat(message).isEqualTo("관계 없음")
    }

    @Test
    @DisplayName("상태 메시지 반환 - PENDING")
    fun `getStatusMessage should return correct message for PENDING`() {
        // when
        val message = matchUtilityService.getStatusMessage(MatchStatus.PENDING)

        // then
        assertThat(message).isEqualTo("매칭 대기 중입니다.")
    }

    @Test
    @DisplayName("상태 메시지 반환 - ACCEPTED")
    fun `getStatusMessage should return correct message for ACCEPTED`() {
        // when
        val message = matchUtilityService.getStatusMessage(MatchStatus.ACCEPTED)

        // then
        assertThat(message).isEqualTo("룸메이트 매칭이 성사되었습니다!")
    }

    @Test
    @DisplayName("상태 메시지 반환 - REJECTED")
    fun `getStatusMessage should return correct message for REJECTED`() {
        // when
        val message = matchUtilityService.getStatusMessage(MatchStatus.REJECTED)

        // then
        assertThat(message).isEqualTo("매칭이 거절되었습니다.")
    }

    @Test
    @DisplayName("Match를 MatchStatusItem으로 변환 - sender가 currentUserId인 경우")
    fun `toMatchStatusItem should convert Match correctly when sender is current user`() {
        // given
        val sender = User("송신자", "sender@test.ac.kr", "pw", Gender.MALE, LocalDate.of(1998, 1, 1), "서울대학교")
        val receiver = User("수신자", "receiver@test.ac.kr", "pw", Gender.MALE, LocalDate.of(1999, 1, 1), "서울대학교")
        sender.verifyStudent()
        receiver.verifyStudent()
        val savedSender = userRepository.save(sender)
        val savedReceiver = userRepository.save(receiver)

        val match = Match.createRequest(savedSender, savedReceiver, BigDecimal.valueOf(0.85))
        match.senderResponse = MatchStatus.ACCEPTED
        match.receiverResponse = MatchStatus.PENDING
        match.matchStatus = MatchStatus.PENDING
        val savedMatch = matchRepository.save(match)

        val senderId = savedSender.id ?: error("송신자 ID가 null입니다.")

        // when
        val statusItem = matchUtilityService.toMatchStatusItem(savedMatch, senderId)

        // then
        assertThat(statusItem.id).isEqualTo(savedMatch.id)
        assertThat(statusItem.senderId).isEqualTo(senderId)
        assertThat(statusItem.receiverId).isEqualTo(savedReceiver.id)
        assertThat(statusItem.matchType).isEqualTo(MatchType.REQUEST)
        assertThat(statusItem.matchStatus).isEqualTo(MatchStatus.PENDING)
        assertThat(statusItem.myResponse).isEqualTo(MatchStatus.ACCEPTED)
        assertThat(statusItem.partnerResponse).isEqualTo(MatchStatus.PENDING)
        assertThat(statusItem.waitingForPartner).isTrue
        assertThat(statusItem.partner.id).isEqualTo(savedReceiver.id)
        assertThat(statusItem.partner.name).isEqualTo("수신자")
    }

    @Test
    @DisplayName("Match를 MatchStatusItem으로 변환 - receiver가 currentUserId인 경우")
    fun `toMatchStatusItem should convert Match correctly when receiver is current user`() {
        // given
        val sender = User("송신자", "sender@test.ac.kr", "pw", Gender.MALE, LocalDate.of(1998, 1, 1), "서울대학교")
        val receiver = User("수신자", "receiver@test.ac.kr", "pw", Gender.MALE, LocalDate.of(1999, 1, 1), "서울대학교")
        sender.verifyStudent()
        receiver.verifyStudent()
        val savedSender = userRepository.save(sender)
        val savedReceiver = userRepository.save(receiver)

        val match = Match.createRequest(savedSender, savedReceiver, BigDecimal.valueOf(0.85))
        match.senderResponse = MatchStatus.PENDING
        match.receiverResponse = MatchStatus.ACCEPTED
        match.matchStatus = MatchStatus.PENDING
        val savedMatch = matchRepository.save(match)

        val receiverId = savedReceiver.id ?: error("수신자 ID가 null입니다.")

        // when
        val statusItem = matchUtilityService.toMatchStatusItem(savedMatch, receiverId)

        // then
        assertThat(statusItem.id).isEqualTo(savedMatch.id)
        assertThat(statusItem.senderId).isEqualTo(savedSender.id)
        assertThat(statusItem.receiverId).isEqualTo(receiverId)
        assertThat(statusItem.myResponse).isEqualTo(MatchStatus.ACCEPTED)
        assertThat(statusItem.partnerResponse).isEqualTo(MatchStatus.PENDING)
        assertThat(statusItem.waitingForPartner).isTrue
        assertThat(statusItem.partner.id).isEqualTo(savedSender.id)
        assertThat(statusItem.partner.name).isEqualTo("송신자")
    }

    @Test
    @DisplayName("Match를 MatchResultItem으로 변환")
    fun `toMatchResultItem should convert Match correctly`() {
        // given
        val sender = User("송신자", "sender@test.ac.kr", "pw", Gender.MALE, LocalDate.of(1998, 1, 1), "서울대학교")
        val receiver = User("수신자", "receiver@test.ac.kr", "pw", Gender.MALE, LocalDate.of(1999, 1, 1), "서울대학교")
        sender.verifyStudent()
        receiver.verifyStudent()
        val savedSender = userRepository.save(sender)
        val savedReceiver = userRepository.save(receiver)

        val match = Match.createRequest(savedSender, savedReceiver, BigDecimal.valueOf(0.85))
        match.matchStatus = MatchStatus.ACCEPTED
        match.confirmedAt = LocalDateTime.now()
        val savedMatch = matchRepository.save(match)

        // when
        val resultItem = matchUtilityService.toMatchResultItem(savedMatch)

        // then
        assertThat(resultItem.id).isEqualTo(savedMatch.id)
        assertThat(resultItem.senderId).isEqualTo(savedSender.id)
        assertThat(resultItem.senderName).isEqualTo("송신자")
        assertThat(resultItem.receiverId).isEqualTo(savedReceiver.id)
        assertThat(resultItem.receiverName).isEqualTo("수신자")
        assertThat(resultItem.matchType).isEqualTo(MatchType.REQUEST)
        assertThat(resultItem.matchStatus).isEqualTo(MatchStatus.ACCEPTED)
        assertThat(resultItem.preferenceScore).isEqualTo(BigDecimal.valueOf(0.85))
        assertThat(resultItem.confirmedAt).isNotNull
    }
}