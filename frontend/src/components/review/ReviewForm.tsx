'use client';

import { useState, useEffect } from "react";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import { StarRating } from "@/components/ui/StarRating";
import { useCreateReview, useUpdateReview } from "@/hooks/useReview";
import { useRouter } from "next/navigation";
import { useToast } from "@/components/ui/Toast";
import { getErrorMessage } from "@/lib/utils/helpers";
import type { ReviewResponse } from "@/types/review";

interface ReviewFormProps {
  matchId: number;
  targetUserId: number;
  onSuccess?: () => void;
  review?: ReviewResponse | null; 
  isEditMode?: boolean; 
}

export function ReviewForm({ matchId, targetUserId, onSuccess, review, isEditMode = false }: ReviewFormProps) {
  const [rating, setRating] = useState(0);
  const [content, setContent] = useState("");
  const [recommend, setRecommend] = useState(true);
  const { createReview, loading: creating, error: createError } = useCreateReview();
  const { updateReview, loading: updating, error: updateError } = useUpdateReview();
  const router = useRouter();
  const { success } = useToast();

  useEffect(() => {
    if (isEditMode && review) {
      setRating(review.rating);
      setContent(review.content || "");
      setRecommend(review.recommend);
    }
  }, [isEditMode, review]);

  const loading = creating || updating;
  const error = createError || updateError;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (rating === 0) {
      return;
    }

    try {
      if (isEditMode && review) {
        await updateReview(review.reviewId, {
          rating,
          content: content || null,
          recommend,
        });
        success("리뷰가 성공적으로 수정되었습니다.", "리뷰 수정 완료");
      } else {
        await createReview({
          matchId: Number(matchId),
          rating,
          content,
          recommend,
        });
        success("리뷰가 성공적으로 제출되었습니다.", "리뷰 제출 완료");
      }
      
      if (onSuccess) {
        onSuccess();
      } else {
        router.push("/reviews/pending");
      }
    } catch (e) {
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          평점
        </label>
        <StarRating rating={rating} onRatingChange={setRating} />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">
          한줄평
        </label>
        <Input
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="리뷰를 남겨주세요."
          className="mt-1"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">
          이 사용자와 다시 매칭하고 싶나요?
        </label>
        <div className="flex gap-4 mt-2">
          <button
            type="button"
            className={`px-4 py-2 rounded-lg transition-colors ${
              recommend ? "bg-blue-600 text-white" : "bg-gray-200 text-gray-700"
            }`}
            onClick={() => setRecommend(true)}
          >
            예
          </button>
          <button
            type="button"
            className={`px-4 py-2 rounded-lg transition-colors ${
              !recommend ? "bg-red-500 text-white" : "bg-gray-200 text-gray-700"
            }`}
            onClick={() => setRecommend(false)}
          >
            아니요
          </button>
        </div>
      </div>

      {error && (
        <div className="text-red-500 text-sm">{error.message}</div>
      )}

      <Button type="submit" disabled={loading} className="w-full">
        {loading ? (isEditMode ? "수정 중..." : "제출 중...") : (isEditMode ? "리뷰 수정" : "리뷰 제출")}
      </Button>
    </form>
  );
}