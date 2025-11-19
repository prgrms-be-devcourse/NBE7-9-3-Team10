import { useState, useEffect, useCallback } from 'react';
import { ReviewService } from '@/lib/services/reviewService';
import {
    ReviewResponse,
    PendingReviewResponse,
    ReviewCreateRequest,
    ReviewUpdateRequest,
} from '@/types/review';
import { ApiError } from '@/types/api';

/**
 * 대기 중인 후기 목록 조회 훅
 */
export const usePendingReviews = () => {
    const [pendingReviews, setPendingReviews] = useState<PendingReviewResponse[]>([]);
    const [loading, setLoading] = useState(false); 
    const [error, setError] = useState<ApiError | null>(null);

    const fetchPendingReviews = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await ReviewService.getPendingReviews();
            const data = Array.isArray(response) 
                ? response 
                : (response as any)?.data || [];
            setPendingReviews(Array.isArray(data) ? data : []);
        } catch (err) {
            setError(err as ApiError);
            setPendingReviews([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchPendingReviews();
    }, [fetchPendingReviews]);

    return { pendingReviews, loading, error, refetch: fetchPendingReviews };
};

/**
 * 단일 후기 조회 훅
 */
export const useReview = (reviewId?: number) => {
    const [review, setReview] = useState<ReviewResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const fetchReview = useCallback(async (id: number) => {
        setLoading(true);
        setError(null);
        try {
            const response = await ReviewService.getReview(id);
            const data = (response as any)?.data || response;
            setReview(data || null);
        } catch (err) {
            setError(err as ApiError);
            setReview(null);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (reviewId) {
            fetchReview(reviewId);
        }
    }, [reviewId, fetchReview]);

    return { review, loading, error, refetch: () => reviewId && fetchReview(reviewId) };
};

/**
 * 매칭별 후기 목록 조회 훅
 */
export const useReviewsByMatch = (matchId?: number) => {
    const [reviews, setReviews] = useState<ReviewResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const fetchReviews = useCallback(async (id: number) => {
        setLoading(true);
        setError(null);
        try {
            const response = await ReviewService.getReviewsByMatch(id);
            const data = Array.isArray(response) 
                ? response 
                : (response as any)?.data || [];
            setReviews(Array.isArray(data) ? data : []);
        } catch (err) {
            setError(err as ApiError);
            setReviews([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (matchId) {
            fetchReviews(matchId);
        }
    }, [matchId, fetchReviews]);

    return { reviews, loading, error, refetch: () => matchId && fetchReviews(matchId) };
};

/**
 * 후기 작성 훅
 */
export const useCreateReview = () => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const createReview = async (request: ReviewCreateRequest) => {
        setLoading(true);
        setError(null);
        try {
            const response = await ReviewService.createReview(request);
            const data = (response as any)?.data || response;
            return data;
        } catch (err) {
            setError(err as ApiError);
            throw err;
        } finally {
            setLoading(false);
        }
    };

    return { createReview, loading, error };
};

/**
 * 후기 수정 훅
 */
export const useUpdateReview = () => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const updateReview = async (reviewId: number, request: ReviewUpdateRequest) => {
        setLoading(true);
        setError(null);
        try {
            const response = await ReviewService.updateReview(reviewId, request);
            const data = (response as any)?.data || response;
            return data;
        } catch (err) {
            setError(err as ApiError);
            throw err;
        } finally {
            setLoading(false);
        }
    };

    return { updateReview, loading, error };
};

/**
 * 후기 삭제 훅
 */
export const useDeleteReview = () => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const deleteReview = async (reviewId: number) => {
        setLoading(true);
        setError(null);
        try {
            await ReviewService.deleteReview(reviewId);
        } catch (err) {
            setError(err as ApiError);
            throw err;
        } finally {
            setLoading(false);
        }
    };

    return { deleteReview, loading, error };
};

/**
 * 재매칭 가능 여부 확인 훅
 */
export const useCanRematch = (matchId?: number) => {
    const [canRematch, setCanRematch] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const checkCanRematch = useCallback(async (id: number) => {
        setLoading(true);
        setError(null);
        try {
            const response = await ReviewService.canRematch(id);
            const data = (response as any)?.data || response;
            setCanRematch(data?.canRematch || false);
        } catch (err) {
            setError(err as ApiError);
            setCanRematch(false);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (matchId) {
            checkCanRematch(matchId);
        }
    }, [matchId, checkCanRematch]);

    return { canRematch, loading, error, refetch: () => matchId && checkCanRematch(matchId) };
};