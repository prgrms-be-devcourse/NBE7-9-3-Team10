package com.unimate.support.seed

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.batch.item.support.CompositeItemWriter
import org.springframework.batch.item.support.IteratorItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
@EnableBatchProcessing
class SeedBatchConfig(
    private val dataSource: DataSource
) {
    private val factory = RandomDataFactory()

    @Bean
    fun seedJob(jobRepository: JobRepository, seedStep: Step) =
        JobBuilder("seedJob", jobRepository)
            .start(seedStep)
            .build()

    @Bean
    fun passwordEncoderProcessor(encoder: PasswordEncoder) =
        ItemProcessor<UserProfileItem, UserProfileItem> { item ->
            item.copy(password = encoder.encode(item.password))
        }

    @Bean
    fun seedStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
        encoder: PasswordEncoder
    ) = StepBuilder("seedStep", jobRepository)
        .chunk<UserProfileItem, UserProfileItem>(1000, txManager)
        .reader(itemReader())
        .processor(passwordEncoderProcessor(encoder))
        .writer(itemWriter())
        .build()

    @Bean
    fun itemReader(): ItemReader<UserProfileItem> {
        val items = (1..1000).map { i ->
            UserProfileItem(
                email = factory.email(i),
                password = "test1234",
                name = factory.name(),
                gender = factory.gender().name,
                birthDate = factory.birthDate(),
                studentVerified = factory.studentVerified(),
                university = factory.university(),
                sleepTime = factory.sleepTime(),
                isPetAllowed = factory.bool(),
                isSmoker = factory.bool(),
                cleaningFrequency = factory.range(1, 5),
                preferredAgeGap = factory.range(0, 3),
                hygieneLevel = factory.range(1, 5),
                isSnoring = factory.snoring(),
                drinkingFrequency = factory.drinkingFrequency(),
                noiseSensitivity = factory.range(1, 5),
                guestFrequency = factory.guestFrequency(),
                mbti = factory.mbti(),
                startUseDate = factory.startUseDate(),
                endUseDate = factory.endUseDate(),
                matchingEnabled = factory.matchingEnabled()
            )
        }
        return IteratorItemReader(items)
    }

    @Bean
    fun usersWriter() = createWriter<UserProfileItem>(
        """
        INSERT INTO users (email, password, name, gender, birth_date, student_verified, university) 
        VALUES (:email, :password, :name, :gender, :birthDate, :studentVerified, :university)
        """.trimIndent()
    )

    @Bean
    fun profilesWriter() = createWriter<UserProfileItem>(
        """
        INSERT INTO user_profile (
          user_id,
          sleep_time, is_pet_allowed, is_smoker,
          cleaning_frequency, preferred_age_gap, hygiene_level,
          is_snoring, drinking_frequency, noise_sensitivity, guest_frequency,
          matching_enabled, mbti,
          start_use_date, end_use_date
        ) VALUES (
          (SELECT id FROM users WHERE email = :email),
          :sleepTime, :isPetAllowed, :isSmoker,
          :cleaningFrequency, :preferredAgeGap, :hygieneLevel,
          :isSnoring, :drinkingFrequency, :noiseSensitivity, :guestFrequency,
          :matchingEnabled, :mbti,
          :startUseDate, :endUseDate
        )
        """.trimIndent()
    )

    @Bean
    fun matchPreferencesWriter() = createWriter<UserProfileItem>(
        """
        INSERT INTO user_match_preference (
          user_id,
          sleep_time, is_pet_allowed, is_smoker,
          cleaning_frequency, preferred_age_gap, hygiene_level,
          is_snoring, drinking_frequency, noise_sensitivity, guest_frequency,
          start_use_date, end_use_date
        ) VALUES (
          (SELECT id FROM users WHERE email = :email),
          :sleepTime, :isPetAllowed, :isSmoker,
          :cleaningFrequency, :preferredAgeGap, :hygieneLevel,
          :isSnoring, :drinkingFrequency, :noiseSensitivity, :guestFrequency,
          :startUseDate, :endUseDate
        )
        """.trimIndent()
    )

    @Bean
    fun itemWriter(): ItemWriter<UserProfileItem> =
        CompositeItemWriter<UserProfileItem>().apply {
            setDelegates(listOf(usersWriter(), profilesWriter(), matchPreferencesWriter())) // 순서 중요: users → profiles → match_preference
        }

    private inline fun <reified T> createWriter(sql: String): ItemWriter<T> =
        JdbcBatchItemWriterBuilder<T>()
            .dataSource(dataSource)
            .sql(sql)
            .beanMapped()
            .build()
}

