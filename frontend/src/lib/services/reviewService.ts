import { api, API_ENDPOINTS } from '@/lib/services/api';
import { ApiResponse } from '@/types/api';
import {
    ReviewCreateRequest,
    ReviewUpdateRequest,
    ReviewResponse,
    PendingReviewResponse,
    CanRematchResponse,
} from '@/types/review';

export class ReviewService {
    // 후기 작성 
    static async createReview(request: ReviewCreateRequest): Promise<ApiResponse<ReviewResponse>> {
        return api.post<ApiResponse<ReviewResponse>>(API_ENDPOINTS.REVIEWS, request);
    }

    // 후기 조회 
    static async getReview(reviewId: number): Promise<ApiResponse<ReviewResponse>> {
        return api.get<ApiResponse<ReviewResponse>>(`${API_ENDPOINTS.REVIEWS}/${reviewId}`);
    }

    // 매칭별 후기 목록 조회 
    static async getReviewsByMatch(matchId: number): Promise<ApiResponse<ReviewResponse[]>> {
        return api.get<ApiResponse<ReviewResponse[]>>(`${API_ENDPOINTS.REVIEWS_BY_MATCH}/${matchId}`);
    }

    // 대기 중인 후기 목록 조회 
    static async getPendingReviews(): Promise<ApiResponse<PendingReviewResponse[]>> {
        return api.get<ApiResponse<PendingReviewResponse[]>>(API_ENDPOINTS.REVIEWS_PENDING);
    }

    // 후기 수정 
    static async updateReview(
        reviewId: number,
        request: ReviewUpdateRequest
    ): Promise<ApiResponse<ReviewResponse>> {
        return api.put<ApiResponse<ReviewResponse>>(`${API_ENDPOINTS.REVIEWS}/${reviewId}`, request);
    }

    // 후기 삭제 
    static async deleteReview(reviewId: number): Promise<ApiResponse<void>> {
        return api.delete<ApiResponse<void>>(`${API_ENDPOINTS.REVIEWS}/${reviewId}`);
    }

    // 재매칭 가능 여부 확인 
    static async canRematch(matchId: number): Promise<ApiResponse<CanRematchResponse>> {
        return api.get<ApiResponse<CanRematchResponse>>(
            `${API_ENDPOINTS.REVIEWS}/match/${matchId}/can-rematch`
        );
    }
}