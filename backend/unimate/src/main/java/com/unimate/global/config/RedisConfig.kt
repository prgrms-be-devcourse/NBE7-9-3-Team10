package com.unimate.global.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

// Duration 확장 함수
private fun Int.minutes() = Duration.ofMinutes(this.toLong())
private fun Int.hours() = Duration.ofHours(this.toLong())

// redis 설정
@Configuration
@EnableCaching
class RedisConfig {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory) =
        RedisTemplate<String, Any>().apply {
            setConnectionFactory(connectionFactory)
            val serializer = JdkSerializationRedisSerializer()
            keySerializer = StringRedisSerializer()
            valueSerializer = serializer
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = serializer
        }

    // matchCandidatesV2(10분), matchCandidatesByFilter(30분), userProfile(1시간) TTL 적용
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val serializer = JdkSerializationRedisSerializer()
        val defaultConfig = createCacheConfig(serializer, 10.minutes())

        val cacheConfigurations = mapOf(
            "matchCandidatesV2" to createCacheConfig(serializer, 10.minutes()),
            "matchCandidatesByFilter" to createCacheConfig(serializer, 30.minutes()),
            "userProfile" to createCacheConfig(serializer, 1.hours())
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    // 캐시 설정 생성 헬퍼 메서드
    private fun createCacheConfig(
        serializer: JdkSerializationRedisSerializer,
        ttl: Duration
    ) = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(ttl)
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
        )
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(serializer)
        )
        .disableCachingNullValues()
}

