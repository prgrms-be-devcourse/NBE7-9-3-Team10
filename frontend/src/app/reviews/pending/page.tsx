'use client';

import PendingReviewList from '@/components/review/PendingReviewList';
import ProtectedRoute from '@/components/auth/ProtectedRoute';

export default function PendingReviewsPage() {
  return (
    <ProtectedRoute>
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold mb-6">대기 중인 후기</h1>
        <PendingReviewList />
      </div>
    </ProtectedRoute>
  );
}