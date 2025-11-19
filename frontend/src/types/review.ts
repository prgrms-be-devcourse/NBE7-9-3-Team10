// 후기 작성 요청
export interface ReviewCreateRequest {
    matchId: number;
    rating: number; // 1~5
    content?: string | null;
    recommend: boolean; // 룸메이트로 추천 여부
}

// 후기 수정 요청
export interface ReviewUpdateRequest {
    rating?: number | null; // 1~5
    content?: string | null;
    recommend?: boolean | null;
}

// 후기 응답
export interface ReviewResponse {
    reviewId: number;
    matchId: number;
    reviewerId: number;
    reviewerName: string;
    revieweeId: number;
    revieweeName: string;
    rating: number;
    content: string | null;
    recommend: boolean;
    canRematch: boolean;
    createdAt: string;
    updatedAt: string;
}

// 대기 중인 후기 응답
export interface PendingReviewResponse {
    matchId: number;
    revieweeId: number;
    revieweeName: string;
    revieweeUniversity: string;
    matchEndDate: string; 
    canCreateReview: boolean;
    remainingDays: number | null;
}

// 재매칭 가능 여부 응답
export interface CanRematchResponse {
    canRematch: boolean;
}