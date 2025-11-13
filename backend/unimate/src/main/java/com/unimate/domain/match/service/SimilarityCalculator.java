package com.unimate.domain.match.service;

import com.unimate.domain.userMatchPreference.entity.UserMatchPreference;
import com.unimate.domain.userProfile.entity.UserProfile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

@Service
public class SimilarityCalculator {

    private static final int MAX_SCORE_SCALE = 5;
    private static final int MIN_SCORE_SCALE = 1;
    private static final double SCALE_RANGE = (double) (MAX_SCORE_SCALE - MIN_SCORE_SCALE);

    // 가중치
    private static final double WEIGHT_SMOKING = 0.20;
    private static final double WEIGHT_SLEEP = 0.20;
    private static final double WEIGHT_CLEANLINESS = 0.20;
    private static final double WEIGHT_AGE = 0.10;
    private static final double WEIGHT_NOISE = 0.10;
    private static final double WEIGHT_PET = 0.10;
    private static final double WEIGHT_LIFESTYLE = 0.10;

    public double calculateSimilarity(UserMatchPreference preference, UserProfile profile) {
        // Null 체크 : 프로필 또는 필수 정보가 없으면 0점 반환
        if (preference == null || profile == null || profile.getUser() == null) return 0.0;

        // 항목별 점수 계산 & 카테고리별로 묶음

        // 흡연 점수
        double smokerScore = calculateBooleanScore(preference.getIsSmoker(), profile.isSmoker());

        // 수면 점수
        double sleepScore = calculateIntegerScore(preference.getSleepTime(), profile.getSleepTime());

        // 반려동물 점수
        double petScore = calculateBooleanScore(preference.getIsPetAllowed(), profile.isPetAllowed());

        // 나이 차이 점수
        double ageGapScore = calculateAgeGapScore(preference.getPreferredAgeGap(), profile.getUser().getBirthDate());

        // 청결 점수 (청소 빈도 + 위생 수준)
        double cleaningFrequencyScore = calculateIntegerScore(preference.getCleaningFrequency(), profile.getCleaningFrequency());
        double hygieneLevelScore = calculateIntegerScore(preference.getHygieneLevel(), profile.getHygieneLevel());
        double cleanlinessScore = (cleaningFrequencyScore + hygieneLevelScore) / 2.0;

        // 소음 점수 (소음 민감도 + 코골이 여부)
        double noiseSensitivityScore = calculateIntegerScore(preference.getNoiseSensitivity(), profile.getNoiseSensitivity());
        double snoringScore = calculateBooleanScore(preference.getIsSnoring(), profile.isSnoring());
        double noiseScore = (noiseSensitivityScore + snoringScore) / 2.0;

        // 생활방식 점수 (음주 빈도 + 방문자 빈도)
        double drinkingFrequencyScore = calculateIntegerScore(preference.getDrinkingFrequency(), profile.getDrinkingFrequency());
        double guestFrequencyScore = calculateIntegerScore(preference.getGuestFrequency(), profile.getGuestFrequency());
        double lifestyleScore = (drinkingFrequencyScore + guestFrequencyScore) / 2.0;


        // 가중치 적용 및 최종 점수 계산
        double finalScore = (smokerScore * WEIGHT_SMOKING) +
                (sleepScore * WEIGHT_SLEEP) +
                (cleanlinessScore * WEIGHT_CLEANLINESS) +
                (ageGapScore * WEIGHT_AGE) +
                (noiseScore * WEIGHT_NOISE) +
                (petScore * WEIGHT_PET) +
                (lifestyleScore * WEIGHT_LIFESTYLE);

        // 소수점 둘째 자리까지 반올림
        return Math.round(finalScore * 100) / 100.0;

    }

    private double calculateIntegerScore(Integer preferenceValue, Integer profileValue) {
        if (preferenceValue == null || profileValue == null) {
            return 0.0;
        }
        // 선호도와 프로필 값이 얼마나 다른지를 계산. 점수가 높을수록 유사함.
        return 1.0 - (Math.abs(preferenceValue - profileValue) / SCALE_RANGE);
    }

    private double calculateBooleanScore(Boolean preferenceValue, Boolean profileValue) {
        if (preferenceValue == null || profileValue == null) {
            return 0.0;
        }
        // 선호도와 프로필 값이 일치하면 1점, 아니면 0점
        return Objects.equals(preferenceValue, profileValue) ? 1.0 : 0.0;
    }

    private double calculateAgeGapScore(Integer preferredAgeBlock, LocalDate targetBirthDate) {
        if (targetBirthDate == null || preferredAgeBlock == null) {
            return 0.0;
        }

        int targetAge = Period.between(targetBirthDate, LocalDate.now()).getYears();
        int targetAgeBlock = getAgeBlock(targetAge);

        if (targetAgeBlock == 0) { // 나이가 유효 범위 밖이면 0점
            return 0.0;
        }

        // 나의 선호 블럭 번호와 상대의 실제 나이 블럭 번호를 비교하여 점수 계산
        return calculateIntegerScore(preferredAgeBlock, targetAgeBlock);
    }

    private int getAgeBlock(int age) {
        if (age >= 20 && age <= 22) return 1;
        if (age >= 23 && age <= 25) return 2;
        if (age >= 26 && age <= 28) return 3;
        if (age >= 29 && age <= 30) return 4;
        if (age >= 31) return 5;
        return 0; // 범위 밖의 나이는 0점 처리
    }


}
