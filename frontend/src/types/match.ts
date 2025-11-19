// 매칭 관련 타입 정의

export interface MatchRecommendationItem {
  receiverId: number;
  name: string;
  age: number;
  university: string;
  mbti: string;
  gender: 'MALE' | 'FEMALE';
  studentVerified: boolean;
  preferenceScore: number;
  isLiked?: boolean;
  matchType?: 'LIKE' | 'REQUEST';
  matchStatus?: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  sleepTime?: number;
  cleaningFrequency?: number;
  isSmoker?: boolean;
  startUseDate?: string;
  endUseDate?: string;
}

export interface MatchRecommendationResponse {
  users: MatchRecommendationItem[];
}

export interface MatchRecommendationDetailResponse {
  receiverId: number;
  email: string;  // 신고 기능을 위한 이메일 추가
  name: string;
  age: number;
  university: string;
  mbti: string;
  gender: 'MALE' | 'FEMALE';
  studentVerified: boolean;
  preferenceScore: number;
  sleepTime?: number;
  cleaningFrequency?: number;
  hygieneLevel?: number;
  noiseSensitivity?: number;
  drinkingFrequency?: number;
  guestFrequency?: number;
  isSmoker?: boolean;
  isPetAllowed?: boolean;
  isSnoring?: boolean;
  startUseDate?: string;
  endUseDate?: string;
  preferredAgeGap?: number;
  birthDate?: string;
  isLiked?: boolean;
  matchType?: 'LIKE' | 'REQUEST';
  matchStatus?: 'PENDING' | 'ACCEPTED' | 'REJECTED';
}

export interface MatchStatusSummary {
  total: number;
  pending: number;
  accepted: number;
  rejected: number;
}

export interface MatchStatusPartnerInfo {
  id: number;
  name: string;
  email: string;
  university: string;
}

export interface MatchStatusItem {
  id: number;
  senderId: number;
  receiverId: number;
  matchType: 'LIKE' | 'REQUEST';
  matchStatus: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  preferenceScore: number;
  createdAt: string;
  confirmedAt?: string;
  message: string;
  partner: MatchStatusPartnerInfo;
}

export interface MatchStatusResponse {
  matches: MatchStatusItem[];
  summary: MatchStatusSummary;
}

export interface MatchResultItem {
  id: number;
  senderId: number;
  senderName: string;
  receiverId: number;
  receiverName: string;
  matchType: 'LIKE' | 'REQUEST';
  matchStatus: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  preferenceScore: number;
  createdAt: string;
  updatedAt: string;
  confirmedAt: string;
  rematchRound?: number;  
  partnerName?: string;
  chatroomId?: number;
}

export interface MatchResultResponse {
  results: MatchResultItem[];
}

export interface MatchFilters {
  sleepPattern?: string;
  cleaningFrequency?: string;
  ageRange?: string;
  startDate?: string;
  endDate?: string;
}

