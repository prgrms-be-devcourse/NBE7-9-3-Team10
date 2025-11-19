"use client";

import { useState } from "react";
import { ReviewFormModal } from "@/components/review/ReviewFormModal";
import { usePendingReviews } from "@/hooks/useReview";
import LoadingSpinner from "@/components/ui/LoadingSpinner";
import Button from "@/components/ui/Button";
import { MessageSquare } from "lucide-react";
import type { PendingReviewResponse } from "@/types/review";

export default function PendingReviewList() {
    const { pendingReviews, loading, error, refetch } = usePendingReviews();
    const [open, setOpen] = useState(false);
    const [selected, setSelected] = useState<PendingReviewResponse | null>(null);

    if (loading) return <LoadingSpinner />;

    const handleCloseModal = () => {
        setOpen(false);
        setSelected(null);
    };

    const handleReviewClick = (item: PendingReviewResponse) => {
        setSelected(item);
        setOpen(true);
    };

    const handleReviewSuccess = () => {
        refetch();
        handleCloseModal();
    };

    if (error) {
        return (
            <div className="text-center py-12">
                <p className="text-red-500 text-lg">에러가 발생했습니다.</p>
                <p className="text-gray-400 text-sm mt-2">{error.message}</p>
            </div>
        );
    }

    if (!pendingReviews || pendingReviews.length === 0) {
        return (
            <div className="text-center py-12">
                <MessageSquare className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500 text-lg">대기 중인 후기가 없습니다.</p>
                <p className="text-gray-400 text-sm mt-2">
                    매칭이 완료되고 일정 기간이 지나면 후기를 작성할 수 있습니다.
                </p>
            </div>
        );
    }

    return (
        <div className="space-y-3">
            {pendingReviews.map((item) => (
                <div
                    key={item.matchId}
                    className="bg-gray-50 rounded-lg p-4 border border-gray-200 hover:shadow-md transition-shadow"
                >
                    <div className="flex flex-col gap-3">
                        {/* 이름 */}
                        <div>
                            <p className="font-bold text-lg text-gray-900 mb-1">
                                {item.revieweeName}
                            </p>
                            <p className="text-sm text-gray-600">
                                {item.revieweeUniversity}
                            </p>
                        </div>

                        {/* 종료일 */}
                        {item.matchEndDate && (
                            <p className="text-xs text-gray-500">
                                종료: {item.matchEndDate}
                            </p>
                        )}

                        {/* 후기 작성까지 남은 기간 */}
                        {!item.canCreateReview && item.remainingDays && (
                            <p className="text-xs text-orange-600 font-medium">
                                후기 작성까지 {item.remainingDays}일 남았습니다.
                            </p>
                        )}

                        {/* 리뷰 작성 버튼 */}
                        <Button
                            variant={item.canCreateReview ? "primary" : "secondary"}
                            size="md"
                            onClick={() => handleReviewClick(item)}
                            disabled={!item.canCreateReview}
                            className="w-full"
                        >
                            리뷰 작성
                        </Button>
                    </div>
                </div>
            ))}

            {/* 후기 작성 모달 */}
            {selected && (
                <ReviewFormModal
                    open={open}
                    onClose={handleCloseModal}
                    matchId={selected.matchId}
                    targetUserId={selected.revieweeId}
                    revieweeName={selected.revieweeName}
                    onSuccess={handleReviewSuccess}
                />
            )}
        </div>
    );
}