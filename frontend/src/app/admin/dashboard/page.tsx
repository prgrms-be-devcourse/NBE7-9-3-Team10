'use client';

import AdminProtectedRoute from '@/components/auth/AdminProtectedRoute';
import AdminHeader from '@/components/layout/AdminHeader';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';

export default function AdminDashboardPage() {
  return (
    <AdminProtectedRoute>
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <AdminHeader />

        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <header className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">대시보드</h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">관리자 대시보드에 오신 것을 환영합니다.</p>
          </header>

          <Card>
            <CardHeader>
              <CardTitle>환영합니다!</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-gray-600 dark:text-gray-400">
                관리자 대시보드에 성공적으로 로그인했습니다.
              </p>
            </CardContent>
          </Card>
        </main>
      </div>
    </AdminProtectedRoute>
  );
}