package com.unimate.global.util

import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

@Component
class VerificationCodeGenerator {
    fun generate6Digits(): String =
        ThreadLocalRandom.current()
            .nextInt(1_000_000)
            .toString()
            .padStart(6, '0')
}