// 유틸리티 헬퍼 함수들

/**
 * 날짜를 YYYY-MM-DD 형식으로 포맷
 */
export const formatDate = (date: string | Date): string => {
  const d = new Date(date);
  return d.toISOString().split('T')[0];
};

/**
 * 날짜를 한국어 형식으로 포맷
 */
export const formatDateKorean = (date: string | Date): string => {
  const d = new Date(date);
  return d.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

/**
 * 상대 시간 표시 (예: "3일 전")
 */
export const getRelativeTime = (date: string | Date): string => {
  const now = new Date();
  const target = new Date(date);
  const diffInMs = now.getTime() - target.getTime();
  const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));
  
  if (diffInDays === 0) return '오늘';
  if (diffInDays === 1) return '어제';
  if (diffInDays < 7) return `${diffInDays}일 전`;
  if (diffInDays < 30) return `${Math.floor(diffInDays / 7)}주 전`;
  if (diffInDays < 365) return `${Math.floor(diffInDays / 30)}개월 전`;
  return `${Math.floor(diffInDays / 365)}년 전`;
};

/**
 * 숫자를 한국어 숫자 형식으로 변환
 */
export const formatNumber = (num: number): string => {
  return num.toLocaleString('ko-KR');
};

/**
 * 문자열을 안전하게 자르기
 */
export const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
};

/**
 * 클래스명을 조건부로 결합
 */
export const cn = (...classes: (string | undefined | null | false)[]): string => {
  return classes.filter(Boolean).join(' ');
};

/**
 * 딥 클론
 */
export const deepClone = <T>(obj: T): T => {
  return JSON.parse(JSON.stringify(obj)) as T;
};

/**
 * 객체에서 undefined 값 제거
 */
export const removeUndefined = <T extends Record<string, unknown>>(obj: T): Partial<T> => {
  const result: Partial<T> = {};
  Object.keys(obj).forEach(key => {
    const value = obj[key];
    if (value !== undefined) {
      (result as Record<string, unknown>)[key] = value;
    }
  });
  return result;
};

/**
 * 디바운스 함수
 */
export const debounce = <T extends (...args: unknown[]) => unknown>(
  func: T,
  delay: number
): ((...args: Parameters<T>) => void) => {
  let timeoutId: NodeJS.Timeout;
  return (...args: Parameters<T>) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func(...args), delay);
  };
};

/**
 * 스로틀 함수
 */
export const throttle = <T extends (...args: unknown[]) => unknown>(
  func: T,
  limit: number
): ((...args: Parameters<T>) => void) => {
  let inThrottle: boolean;
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
};

/**
 * 에러 객체에서 메시지 추출
 */
export const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message;
  }
  
  if (typeof error === 'string') {
    return error;
  }
  
  if (error && typeof error === 'object') {
    if ('message' in error) {
      return String(error.message);
    }
    
    // AxiosError 형태 확인
    if ('response' in error) {
      const axiosError = error as any;
      const responseData = axiosError.response?.data;
      if (responseData?.message) {
        return responseData.message;
      }
      if (responseData?.error) {
        return responseData.error;
      }
      return `서버 오류 (${axiosError.response?.status || '알 수 없음'})`;
    }
    
    // 빈 객체인 경우
    if (Object.keys(error).length === 0) {
      return '알 수 없는 오류가 발생했습니다.';
    }
  }
  
  return '알 수 없는 오류가 발생했습니다.';
};

/**
 * 생년월일로부터 나이 계산
 */
export const calculateAge = (birthDate: string): number => {
  const today = new Date();
  const birth = new Date(birthDate);
  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();
  
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--;
  }
  
  return age;
};

/**
 * 나이를 나이대 value로 변환
 * preferredAgeRange: 20-22(1), 23-25(2), 26-28(3), 29-30(4), 31+(5)
 */
export const getAgeRangeFromAge = (age: number): number => {
  if (age >= 20 && age <= 22) return 1;
  if (age >= 23 && age <= 25) return 2;
  if (age >= 26 && age <= 28) return 3;
  if (age >= 29 && age <= 30) return 4;
  if (age >= 31) return 5;
  return 1; // 기본값 (20-22)
};