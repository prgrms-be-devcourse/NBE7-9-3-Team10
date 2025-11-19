'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import AdminAuthService from '@/lib/services/AdminAuthService';
import Link from 'next/link';

const AdminLoginForm = () => {
  const router = useRouter();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  /** ------------------ 로그인 제출 ------------------ */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const newErrors: Record<string, string> = {};
    if (!email) newErrors.email = '이메일을 입력해주세요.';
    if (!password) newErrors.password = '비밀번호를 입력해주세요.';
    setErrors(newErrors);
    if (Object.keys(newErrors).length > 0) return;

    setErrors({});
    setLoading(true);

    try {
      await AdminAuthService.login({ email, password });
      router.push('/admin/dashboard'); // ✅ 관리자 대시보드로 이동 유지
    } catch (err) {
      const apiError = err as any;

      // ✅ 백엔드에서 온 message를 우선적으로 사용 (일반 사용자와 동일)
      const backendMessage =
        apiError.response?.data?.message ||
        apiError.message ||
        '로그인 중 오류가 발생했습니다.';

      if (apiError.response?.status === 404) {
        setErrors({ email: backendMessage });
      } else if (apiError.response?.status === 401) {
        setErrors({ password: backendMessage });
      } else {
        setErrors({ form: backendMessage });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow p-8">
      <div className="flex flex-col items-center mb-6">
        <div className="bg-red-600 dark:bg-red-500 text-white w-12 h-12 rounded-full flex items-center justify-center font-bold text-xl mb-3">
          A
        </div>
        <h2 className="text-xl font-semibold mb-1 text-gray-900 dark:text-white">관리자 로그인</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 text-center">
          관리자 권한으로 시스템에 접근합니다.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* 이메일 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            관리자 이메일
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="admin@unimate.com"
            className={`w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white border rounded-lg px-3 py-2 placeholder:text-gray-400 dark:placeholder:text-gray-500 ${
              errors.email ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
            } focus:ring-2 focus:ring-red-500 dark:focus:ring-red-400`}
          />
          {errors.email && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.email}</p>}
        </div>

        {/* 비밀번호 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            비밀번호
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호를 입력하세요"
            className={`w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white border rounded-lg px-3 py-2 placeholder:text-gray-400 dark:placeholder:text-gray-500 ${
              errors.password ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
            } focus:ring-2 focus:ring-red-500 dark:focus:ring-red-400`}
          />
          {errors.password && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.password}</p>}
        </div>

        {/* 에러 메시지 */}
        {errors.form && <p className="text-red-500 dark:text-red-400 text-sm">{errors.form}</p>}

        {/* 로그인 버튼 */}
        <button
          type="submit"
          disabled={loading}
          className="w-full bg-red-600 dark:bg-red-500 text-white py-2 rounded-lg mt-2 hover:bg-red-700 dark:hover:bg-red-600 disabled:bg-gray-400 dark:disabled:bg-gray-600"
        >
          {loading ? '로그인 중...' : '관리자 로그인'}
        </button>

        {/* 회원가입 링크 */}
        <div className="text-center text-sm text-gray-600 dark:text-gray-400 mt-6">
          관리자 계정이 없으신가요?{' '}
          <Link href="/admin/signup" className="text-red-600 dark:text-red-400 hover:underline font-medium">
            관리자 회원가입
          </Link>
        </div>

        <div className="text-center text-sm text-gray-600 dark:text-gray-400 mt-2">
          일반 사용자이신가요?{' '}
          <Link href="/login" className="text-blue-600 dark:text-blue-400 hover:underline font-medium">
            사용자 로그인
          </Link>
        </div>
      </form>
    </div>
  );
};

export default AdminLoginForm;