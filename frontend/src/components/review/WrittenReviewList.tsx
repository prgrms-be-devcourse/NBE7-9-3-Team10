"use client";

import { useState, useEffect } from "react";
import LoadingSpinner from "@/components/ui/LoadingSpinner";
import { Star, MessageSquare, ThumbsUp, ThumbsDown, User, RefreshCw, Trash2, Edit2 } from "lucide-react"; 
import type { ReviewResponse } from "@/types/review";
import { ReviewService } from "@/lib/services/reviewService";
import { MatchService } from "@/lib/services/matchService";
import { useAuth } from "@/contexts/AuthContext";
import Button from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { useDeleteReview } from "@/hooks/useReview";
import { getErrorMessage } from "@/lib/utils/helpers";
import { ReviewFormModal } from "./ReviewFormModal"; 

interface MatchWithReviews {
    matchId: number;
    partnerName: string;
    partnerUniversity: string;
    reviews: ReviewResponse[];
    myReview?: ReviewResponse;
    partnerReview?: ReviewResponse;
    canRematch?: boolean;
    hasPendingRematch?: boolean; 
}

export default function WrittenReviewList() {
    const { user } = useAuth();
    const { success, error: showError } = useToast();
    const { deleteReview, loading: deleting } = useDeleteReview();
    const [matchesWithReviews, setMatchesWithReviews] = useState<MatchWithReviews[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [rematchingMatchId, setRematchingMatchId] = useState<number | null>(null);
    const [deletingReviewId, setDeletingReviewId] = useState<number | null>(null);
    const [editingReview, setEditingReview] = useState<ReviewResponse | null>(null);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    useEffect(() => {
        const fetchWrittenReviews = async () => {
            if (!user?.userId) {
                setLoading(false);
                return;
            }

            let timeoutId: NodeJS.Timeout | null = null;
            let isUnauthorized = false;

            try {
                setLoading(true);
                setError(null);

                // 타임아웃 추가 (10초)
                timeoutId = setTimeout(() => {
                    setError("요청 시간이 초과되었습니다.");
                    setLoading(false);
                }, 10000);

                const matchStatusResponse = await MatchService.getMatchStatus();
                
                if (timeoutId) {
                    clearTimeout(timeoutId);
                    timeoutId = null;
                }
                
                const matches = matchStatusResponse.data?.matches || 
                               (matchStatusResponse as any)?.matches || 
                               matchStatusResponse.data || 
                               [];
                
                // ACCEPTED 상태의 매칭만 필터링
                const acceptedMatches = Array.isArray(matches) 
                    ? matches.filter((m: any) => m.matchStatus === 'ACCEPTED' || m.status === 'ACCEPTED')
                    : [];

                const matchesData: MatchWithReviews[] = [];

                for (const match of acceptedMatches) {
                    try {
                        const matchId = match.id || match.matchId;
                        if (!matchId) continue;

                        const currentUserId = user.userId;
                        
                        // 파트너 정보는 match 객체에서 직접 가져오기
                        const partnerInfo = match.partner || match.partnerInfo;
                        const partnerName = partnerInfo?.name || 
                                          (match.senderId === currentUserId ? match.receiverName : match.senderName) ||
                                          "알 수 없음";
                        const partnerUniversity = partnerInfo?.university || "";
                        const partnerId = match.senderId === currentUserId ? match.receiverId : match.senderId;

                        // 리뷰 조회 (리뷰가 없어도 매칭은 표시)
                        let reviews: ReviewResponse[] = [];
                        try {
                            const reviewsResponse = await ReviewService.getReviewsByMatch(matchId);
                            reviews = Array.isArray(reviewsResponse) 
                                ? reviewsResponse 
                                : (reviewsResponse as any)?.data || [];
                        } catch (err) {
                            console.error(`매칭 ${matchId}의 리뷰 조회 실패:`, err);
                        }
                        
                        const myReview = reviews.find((r: ReviewResponse) => r.reviewerId === currentUserId);
                        const partnerReview = reviews.find((r: ReviewResponse) => r.reviewerId !== currentUserId);
                        
                        // 재매칭 가능 여부 확인 (리뷰가 없어도 확인)
                        let canRematch = false;
                        try {
                            const canRematchResponse = await ReviewService.canRematch(matchId);
                            canRematch = (canRematchResponse as any)?.data?.canRematch || 
                                        (canRematchResponse as any)?.canRematch || 
                                        false;
                        } catch (err) {
                            console.error(`재매칭 가능 여부 확인 실패:`, err);
                        }

                        // 같은 파트너와의 PENDING 상태 재매칭이 있는지 확인
                        const hasPendingRematch = matches.some((m: any) => {
                            const mPartnerId = m.senderId === currentUserId ? m.receiverId : m.senderId;
                            return mPartnerId === partnerId && 
                                   (m.matchStatus === 'PENDING' || m.status === 'PENDING') &&
                                   m.id !== matchId; // 원본 매칭 제외
                        });

                        // 리뷰가 하나라도 있거나, 재매칭 가능한 경우 표시
                        if (myReview || partnerReview || canRematch) {
                            matchesData.push({
                                matchId,
                                partnerName,
                                partnerUniversity,
                                reviews: reviews,
                                myReview,
                                partnerReview,
                                canRematch: canRematch && !hasPendingRematch, 
                                hasPendingRematch, 
                            });
                        }
                    } catch (err: any) {
                        if (err?.status === 401) {
                            isUnauthorized = true;
                            // api.ts 인터셉터에서 이미 리다이렉트 처리 중
                            // 상태 업데이트하지 않고 리턴
                            if (timeoutId) {
                                clearTimeout(timeoutId);
                            }
                            return;
                        }
                        console.error(`매칭 ${match.id} 처리 실패:`, err);
                    }
                }

                setMatchesWithReviews(matchesData);
            } catch (err: any) {
                if (err?.status === 401) {
                    // api.ts 인터셉터에서 이미 리다이렉트 처리 중
                    // 상태 업데이트하지 않고 리턴
                    if (timeoutId) {
                        clearTimeout(timeoutId);
                    }
                    return;
                }
                
                console.error("작성된 리뷰 조회 실패:", err);
                setError(err.message || "리뷰를 불러오는 중 오류가 발생했습니다.");
            } finally {
                if (timeoutId) {
                    clearTimeout(timeoutId);
                }
                if (!isUnauthorized) {
                    setLoading(false);
                }
            }
        };

        fetchWrittenReviews();

        return () => {
            // cleanup은 비동기 함수 내부의 timeoutId에 접근할 수 없으므로
            // 별도로 관리해야 하지만, 현재 구조에서는 fetchWrittenReviews가 완료되면
            // 자동으로 정리되므로 큰 문제는 없음
        };
    }, [user?.userId]);

    const handleRematch = async (matchId: number) => {
        if (!confirm("정말 재매칭을 요청하시겠습니까?")) {
            return;
        }

        try {
            setRematchingMatchId(matchId);
            await MatchService.requestRematch(matchId);
            success("재매칭 요청이 전송되었습니다.", "재매칭 요청 완료");
            // 목록 새로고침
            window.location.reload();
        } catch (err: any) {
            console.error("재매칭 요청 실패:", err);
            // 에러 상세 로깅
            if (err && typeof err === 'object') {
                console.error('에러 상세:', {
                    message: err.message,
                    status: err.status,
                    response: err.response,
                    data: err.response?.data,
                });
            }
            
            const errorMessage = getErrorMessage(err);
            
            // 409 Conflict는 이미 재매칭이 존재하는 경우 - 정보 메시지로 표시
            if (err?.status === 409) {
                success(
                    errorMessage || "이미 재매칭 요청이 진행 중입니다. 채팅방에서 확인해주세요.",
                    "재매칭 요청"
                );
                // 목록 새로고침하여 최신 상태 반영
                window.location.reload();
            } else {
                showError(errorMessage || "재매칭 요청에 실패했습니다.", "재매칭 실패");
            }
        } finally {
            setRematchingMatchId(null);
        }
    };

    const handleDeleteReview = async (reviewId: number) => {
        if (!confirm("정말 이 리뷰를 삭제하시겠습니까?\n삭제 후에는 되돌릴 수 없습니다.")) {
            return;
        }

        try {
            setDeletingReviewId(reviewId);
            await deleteReview(reviewId);
            success("리뷰가 삭제되었습니다.", "리뷰 삭제 완료");
            // 목록 새로고침
            window.location.reload();
        } catch (err: any) {
            console.error("리뷰 삭제 실패:", err);
            const errorMessage = getErrorMessage(err);
            showError(errorMessage || "리뷰 삭제에 실패했습니다.", "리뷰 삭제 실패");
        } finally {
            setDeletingReviewId(null);
        }
    };

    const handleEditReview = (review: ReviewResponse) => {
        setEditingReview(review);
        setIsEditModalOpen(true);
    };

    const handleEditSuccess = () => {
        setIsEditModalOpen(false);
        setEditingReview(null);
        // 목록 새로고침
        window.location.reload();
    };

    if (loading) return <LoadingSpinner />;

    if (error) {
        return (
            <div className="text-center py-12">
                <p className="text-red-500 text-lg">에러가 발생했습니다.</p>
                <p className="text-gray-400 text-sm mt-2">{error}</p>
            </div>
        );
    }

    if (matchesWithReviews.length === 0) {
        return (
            <div className="text-center py-12">
                <MessageSquare className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500 text-lg">작성된 리뷰가 없습니다.</p>
                <p className="text-gray-400 text-sm mt-2">
                    작성한 리뷰가 여기에 표시됩니다.
                </p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {matchesWithReviews.map((matchData) => (
                <div
                    key={matchData.matchId}
                    className="bg-gray-50 rounded-lg p-4 border border-gray-200"
                >
                    <div className="flex items-center gap-3 mb-3">
                        <User className="w-5 h-5 text-gray-500" />
                        <div className="flex-1">
                            <p className="font-bold text-lg text-gray-900">
                                {matchData.partnerName}
                            </p>
                            {matchData.partnerUniversity && (
                                <p className="text-sm text-gray-600">
                                    {matchData.partnerUniversity}
                                </p>
                            )}
                        </div>
                    </div>

                    {/* 리뷰가 없는 경우 안내 */}
                    {!matchData.myReview && !matchData.partnerReview && (
                        <div className="bg-blue-50 rounded-lg p-3 border border-blue-200 mb-3">
                            <p className="text-sm text-blue-700">
                                아직 작성된 리뷰가 없습니다.
                            </p>
                        </div>
                    )}

                    <div className="space-y-3">
                        {/* 내가 작성한 리뷰 */}
                        {matchData.myReview && (
                            <div className="bg-white rounded-lg p-3 border border-blue-200">
                                <div className="flex items-center justify-between mb-2">
                                    <p className="text-sm font-semibold text-blue-700">내가 작성한 리뷰</p>
                                    <div className="flex items-center gap-2">
                                        <div className="flex items-center gap-1">
                                            {Array.from({ length: 5 }).map((_, i) => (
                                                <Star
                                                    key={i}
                                                    size={16}
                                                    fill={i < matchData.myReview!.rating ? "currentColor" : "none"}
                                                    className={
                                                        i < matchData.myReview!.rating
                                                            ? "text-yellow-400"
                                                            : "text-gray-300"
                                                    }
                                                />
                                            ))}
                                        </div>
                                        {/* 수정 버튼 */}
                                        <button
                                            onClick={() => handleEditReview(matchData.myReview!)}
                                            className="p-1 text-gray-400 hover:text-blue-600 transition-colors"
                                            title="리뷰 수정"
                                        >
                                            <Edit2 className="w-4 h-4" />
                                        </button>
                                        <button
                                            onClick={() => handleDeleteReview(matchData.myReview!.reviewId)}
                                            disabled={deletingReviewId === matchData.myReview!.reviewId || deleting}
                                            className="p-1 text-gray-400 hover:text-red-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                            title="리뷰 삭제"
                                        >
                                            {deletingReviewId === matchData.myReview!.reviewId ? (
                                                <Trash2 className="w-4 h-4 animate-pulse" />
                                            ) : (
                                                <Trash2 className="w-4 h-4" />
                                            )}
                                        </button>
                                    </div>
                                </div>
                                {matchData.myReview.content && (
                                    <p className="text-sm text-gray-700 mt-2">
                                        {matchData.myReview.content}
                                    </p>
                                )}
                                <div className="flex items-center gap-2 mt-2">
                                    {matchData.myReview.recommend ? (
                                        <ThumbsUp className="w-4 h-4 text-green-600" />
                                    ) : (
                                        <ThumbsDown className="w-4 h-4 text-red-600" />
                                    )}
                                    <span className="text-xs text-gray-500">
                                        {matchData.myReview.recommend ? "추천" : "비추천"}
                                    </span>
                                </div>
                            </div>
                        )}

                        {/* 상대방이 작성한 리뷰 */}
                        {matchData.partnerReview && (
                            <div className="bg-white rounded-lg p-3 border border-gray-200">
                                <div className="flex items-center justify-between mb-2">
                                    <p className="text-sm font-semibold text-gray-700">
                                        {matchData.partnerName}님이 작성한 리뷰
                                    </p>
                                    <div className="flex items-center gap-1">
                                        {Array.from({ length: 5 }).map((_, i) => (
                                            <Star
                                                key={i}
                                                size={16}
                                                fill={i < matchData.partnerReview!.rating ? "currentColor" : "none"}
                                                className={
                                                    i < matchData.partnerReview!.rating
                                                        ? "text-yellow-400"
                                                        : "text-gray-300"
                                                }
                                            />
                                        ))}
                                    </div>
                                </div>
                                {matchData.partnerReview.content && (
                                    <p className="text-sm text-gray-700 mt-2">
                                        {matchData.partnerReview.content}
                                    </p>
                                )}
                                <div className="flex items-center gap-2 mt-2">
                                    {matchData.partnerReview.recommend ? (
                                        <ThumbsUp className="w-4 h-4 text-green-600" />
                                    ) : (
                                        <ThumbsDown className="w-4 h-4 text-red-600" />
                                    )}
                                    <span className="text-xs text-gray-500">
                                        {matchData.partnerReview.recommend ? "추천" : "비추천"}
                                    </span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* 재매칭 버튼 */}
                    {matchData.canRematch && (
                        <div className="mt-4 pt-3 border-t border-gray-200">
                            <Button
                                variant="primary"
                                size="md"
                                onClick={() => handleRematch(matchData.matchId)}
                                disabled={rematchingMatchId === matchData.matchId || matchData.hasPendingRematch}
                                className="w-full"
                            >
                                {rematchingMatchId === matchData.matchId ? (
                                    <>
                                        <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                                        재매칭 요청 중...
                                    </>
                                ) : (
                                    <>
                                        <RefreshCw className="w-4 h-4 mr-2" />
                                        재매칭 요청
                                    </>
                                )}
                            </Button>
                        </div>
                    )}

                    {/* PENDING 상태의 재매칭이 있는 경우 안내 메시지 */}
                    {matchData.hasPendingRematch && !matchData.canRematch && (
                        <div className="mt-4 pt-3 border-t border-gray-200">
                            <div className="bg-blue-50 rounded-lg p-3 border border-blue-200">
                                <p className="text-sm text-blue-700 font-medium">
                                    재매칭 요청이 진행 중입니다. 채팅방에서 확인해주세요.
                                </p>
                            </div>
                        </div>
                    )}
                </div>
            ))}

            {/* 수정 모달 */}
            {editingReview && (
                <ReviewFormModal
                    open={isEditModalOpen}
                    onClose={() => {
                        setIsEditModalOpen(false);
                        setEditingReview(null);
                    }}
                    matchId={editingReview.matchId}
                    targetUserId={editingReview.revieweeId}
                    revieweeName={editingReview.revieweeName}
                    review={editingReview}
                    isEditMode={true}
                    onSuccess={handleEditSuccess}
                />
            )}
        </div>
    );
}
