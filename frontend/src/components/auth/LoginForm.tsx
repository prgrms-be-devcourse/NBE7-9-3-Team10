'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { ApiError, ERROR_CODES } from '@/types/api';
import { useAuth } from '@/contexts/AuthContext';

const LoginForm = () => {
  const router = useRouter();
  const { login } = useAuth();

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
      await login(email, password);
      router.push('/');
    } catch (err) {
      const apiError = err as any;

      // ✅ 백엔드에서 온 message를 우선적으로 사용
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
        <div className="bg-blue-600 dark:bg-blue-500 text-white w-12 h-12 rounded-full flex items-center justify-center font-bold text-xl mb-3">
          U
        </div>
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-1">로그인</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 text-center">
          UniMate에 로그인하여 룸메이트를 찾아보세요
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* 이메일 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">이메일</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="email@uni.ac.kr"
            className={`w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white border rounded-lg px-3 py-2 placeholder:text-gray-400 dark:placeholder:text-gray-500 ${errors.email ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
              } focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400`}
          />
          {errors.email && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.email}</p>}
        </div>

        {/* 비밀번호 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호를 입력하세요"
            className={`w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white border rounded-lg px-3 py-2 placeholder:text-gray-400 dark:placeholder:text-gray-500 ${errors.password ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
              } focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400`}
          />
          {errors.password && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.password}</p>}
        </div>

        {/* 에러 메시지 */}
        {errors.form && <p className="text-red-500 dark:text-red-400 text-sm">{errors.form}</p>}

        {/* 로그인 버튼 */}
        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 dark:bg-blue-500 text-white py-2 rounded-lg mt-2 hover:bg-blue-700 dark:hover:bg-blue-600 disabled:bg-gray-400 dark:disabled:bg-gray-600"
        >
          {loading ? '로그인 중...' : '로그인'}
        </button>

        {/* 회원가입 링크 */}
        <div className="text-center text-sm text-gray-600 dark:text-gray-400 mt-6">
          계정이 없으신가요?{' '}
          <Link href="/register" className="text-blue-600 dark:text-blue-400 hover:underline font-medium">
            회원가입
          </Link>
        </div>
      </form>
    </div>
  );
};

export default LoginForm;
