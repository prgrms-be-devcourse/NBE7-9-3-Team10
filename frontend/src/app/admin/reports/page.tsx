'use client';

import ReportList from '@/components/admin/ReportList';
import AdminProtectedRoute from '@/components/auth/AdminProtectedRoute';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import AdminHeader from '@/components/layout/AdminHeader';

export default function AdminReportsPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <AdminHeader />
      <AdminProtectedRoute>
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="max-w-6xl mx-auto">
            <header className="mb-8">
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white">신고 관리</h1>
              <p className="text-gray-600 dark:text-gray-400 mt-1">사용자 신고 내역을 확인하고 처리합니다.</p>
            </header>

            <Card>
              <CardHeader>
                <CardTitle>신고 목록</CardTitle>
                <CardDescription>
                  최신 신고 내역부터 표시됩니다. 각 항목을 클릭하여 상세 내용을 확인하세요.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <ReportList />
              </CardContent>
            </Card>
          </div>
        </main>
      </AdminProtectedRoute>
    </div>
  );
}
