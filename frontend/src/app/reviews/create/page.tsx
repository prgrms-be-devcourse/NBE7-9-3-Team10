'use client';

import { Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { ReviewForm } from '@/components/review/ReviewForm';
import ProtectedRoute from '@/components/auth/ProtectedRoute';
import LoadingSpinner from '@/components/ui/LoadingSpinner';
import { Card } from '@/components/ui/Card';

function CreateReviewContent() {
    const searchParams = useSearchParams();
    const matchId = searchParams.get('matchId');
    const targetUserId = searchParams.get('targetUserId'); // 1. targetUserId 가져오기

    // URL에 필수 파라미터가 없는 경우 에러 처리
    if (!matchId || !targetUserId) {
        return (
            <div className="container mx-auto px-4 py-8 text-center">
                <Card className="p-8">
                    <h2 className="text-xl font-semibold text-red-500">잘못된 접근입니다.</h2>
                    <p className="mt-2 text-gray-600">
                        후기 작성을 위한 정보가 올바르지 않습니다.
                    </p>
                </Card>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-4 py-8 max-w-2xl">
            <h1 className="text-2xl font-bold mb-6">후기 작성</h1>
            <Card className="p-6 sm:p-8">
                {/* 2. ReviewForm에 matchId와 targetUserId 전달 */}
                <ReviewForm
                    matchId={matchId}
                    targetUserId={targetUserId}
                />
            </Card>
        </div>
    );
}

export default function CreateReviewPage() {
    return (
        <ProtectedRoute>
            <Suspense fallback={<LoadingSpinner />}>
                <CreateReviewContent />
            </Suspense>
        </ProtectedRoute>
    );
}
