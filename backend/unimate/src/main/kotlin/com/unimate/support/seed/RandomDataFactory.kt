package com.unimate.support.seed

import com.unimate.domain.user.user.entity.Gender
import net.datafaker.Faker
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random

class RandomDataFactory {
    private val faker = Faker(Locale("ko"))
    private val random = Random.Default

    fun email(idx: Int) = "user$idx@unimate.ac.kr"

    fun name() = faker.name().fullName()

    fun gender() = if (random.nextBoolean()) Gender.MALE else Gender.FEMALE

    fun birthDate() = LocalDate.of(
        1990 + random.nextInt(10),
        1 + random.nextInt(12),
        1 + random.nextInt(28)
    )

    fun studentVerified() = true

    fun university(): String {
        val universities = listOf(
            "Unimate",
            "Korea Univ.",
            "Yonsei Univ.",
            "Sogang Univ.",
            "Hanyang Univ."
        )
        return universities.random(random)
    }

    fun sleepTime() = random.nextInt(1, 6) // 1~5

    fun bool() = random.nextBoolean()

    fun range(min: Int, max: Int) = random.nextInt(min, max + 1)

    fun mbti(): String {
        val mbtis = listOf("INTP", "ENTP", "ENFP", "ISTJ", "ISFJ", "INFJ", "ESTJ", "ESFP")
        return mbtis.random(random)
    }

    fun snoring() = random.nextBoolean()

    fun drinkingFrequency() = range(0, 5)

    fun guestFrequency() = range(0, 5)

    fun matchingEnabled() = random.nextBoolean()

    fun startUseDate() = LocalDate.now().minusMonths(range(0, 6).toLong())

    fun endUseDate() = LocalDate.now().plusMonths(range(1, 6).toLong())
}

