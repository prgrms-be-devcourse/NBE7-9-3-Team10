package com.unimate.domain.match.service

import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userProfile.entity.UserProfile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.data.elasticsearch.core.query.StringQuery
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimilarityCalculatorTest {

    @Autowired
    private lateinit var similarityCalculator: SimilarityCalculator

    @MockitoBean
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    @Test
    @DisplayName("Elasticsearch function_score 쿼리 생성 및 점수 정규화 테스트")
    fun `calculateSimilarity with function_score should generate correct query and normalize score`() {
        // given
        val preferenceUser = User("prefUser", "pref@test.ac.kr", "pw", Gender.FEMALE, LocalDate.of(2000, 1, 1), "Test Uni")
        // 일부 항목만 선호도 설정 (isPetAllowed, cleaningFrequency 등은 null)
        val preference = UserMatchPreference(
            preferenceUser,
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            sleepTime = 4,          // weight: 0.2
            isSmoker = false,       // weight: 0.2
            isPetAllowed = null,
            cleaningFrequency = null,
            preferredAgeGap = 1,    // weight: 0.1
            hygieneLevel = 4,       // weight: 0.2 / 2 = 0.1
            isSnoring = false,      // weight: 0.1 / 2 = 0.05
            drinkingFrequency = 3,  // weight: 0.1 / 2 = 0.05
            noiseSensitivity = 2,   // weight: 0.1 / 2 = 0.05
            guestFrequency = 2      // weight: 0.1 / 2 = 0.05
        )
        // 총 가중치: 0.2 + 0.2 + 0.1 + 0.1 + 0.05 + 0.05 + 0.05 + 0.05 = 0.8

        val rawScoreFromEs = 0.75f // ES가 반환할 가중치가 적용된 점수 (정규화 전)

        val queryCaptor = ArgumentCaptor.forClass(Query::class.java)
        val searchHit: SearchHit<UserProfile> = mock(SearchHit::class.java) as SearchHit<UserProfile>
        val searchHits: SearchHits<UserProfile> = mock(SearchHits::class.java) as SearchHits<UserProfile>
        `when`(elasticsearchOperations.search(any<Query>(), ArgumentMatchers.eq(UserProfile::class.java))).thenReturn(searchHits)
        `when`(searchHits.totalHits).thenReturn(1L)
        `when`(searchHits.getSearchHit(0)).thenReturn(searchHit)
        `when`(searchHit.score).thenReturn(rawScoreFromEs)

        // when
        val finalScore = similarityCalculator.calculateSimilarityWithElasticsearch(preference)

        // then
        // 1. 점수 정규화 검증
        // 예상 점수 = (ES 점수 / 총 가중치) 반올림 = (0.75 / 0.8) = 0.9375 -> 0.94
        assertThat(finalScore).isEqualTo(0.94)

        // 2. 생성된 쿼리 구조 검증
        verify(elasticsearchOperations).search(queryCaptor.capture(), ArgumentMatchers.eq(UserProfile::class.java))
        val capturedQuery = (queryCaptor.value as StringQuery).source
        
        assertThat(capturedQuery).contains("function_score")
        assertThat(capturedQuery).contains("gauss") // int 항목에 대해 gauss 함수 사용
        assertThat(capturedQuery).contains("filter") // bool 항목에 대해 filter 함수 사용
        assertThat(capturedQuery).contains("\"weight\"") // 모든 함수에 weight 적용
        assertThat(capturedQuery).contains("\"origin\":4") // sleepTime 선호도 값 확인
        assertThat(capturedQuery).contains("\"term\":{\"isSmoker\":false}") // isSmoker 선호도 값 확인
        
        // null로 설정된 선호도는 쿼리에 포함되지 않아야 함
        assertThat(capturedQuery).doesNotContain("isPetAllowed")
        assertThat(capturedQuery).doesNotContain("cleaningFrequency")
    }

    @Test
    @DisplayName("Kotlin boolean getter 테스트 (완벽 일치)")
    fun `calculateSimilarity with Kotlin boolean getters should succeed`() {
        // given
        val preferenceUser = User("prefUser", "pref@test.ac.kr", "pw", Gender.FEMALE, LocalDate.of(2000, 1, 1), "Test Uni")
        val profileUser = User("profUser", "prof@test.ac.kr", "pw", Gender.FEMALE, LocalDate.now().minusYears(21), "Test Uni")

        val preference = UserMatchPreference(
            preferenceUser,
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            3, true, false, 4, 1, 4, false, 2, 3, 2
        )

        val profile = UserProfile(
            profileUser,
            3, true, false, 4, 1, 4, false, 2, 3, 2, "INFP",
            LocalDate.now(), LocalDate.now().plusMonths(6),
            true
        )

        // when
        val similarity = similarityCalculator.calculateSimilarity(preference, profile)

        // then
        assertThat(similarity).isEqualTo(1.0)
    }

    @Test
    @DisplayName("선호도와 프로필이 완전히 다를 때 0점 반환 테스트")
    fun `calculateSimilarity with different values should return zero`() {
        // given
        val preferenceUser = User("prefUser2", "pref2@test.ac.kr", "pw", Gender.MALE, LocalDate.of(1995, 1, 1), "Test Uni")
        val profileUser = User("profUser2", "prof2@test.ac.kr", "pw", Gender.MALE, LocalDate.now().minusYears(21), "Test Uni")

        val preference = UserMatchPreference(
            preferenceUser,
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            5, true, false, 5, 5, 5, false, 5, 5, 5
        )

        val profile = UserProfile(
            profileUser,
            1, false, true, 1, 1, 1, true, 1, 1, 1, "ESTJ",
            LocalDate.now(), LocalDate.now().plusMonths(6),
            true
        )

        // when
        val similarity = similarityCalculator.calculateSimilarity(preference, profile)

        // then
        assertThat(similarity).isEqualTo(0.0)
    }
}
