'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import AdminAuthService from '@/lib/services/AdminAuthService';
import Link from 'next/link';

const AdminSignupForm = () => {
  const router = useRouter();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [name, setName] = useState('');

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  // 비밀번호 실시간 검증
  const handlePasswordChange = (value: string) => {
    setPassword(value);
    const newErrors = { ...errors };
    
    if (value.length < 8) {
      newErrors.password = '비밀번호는 최소 8자 이상이어야 합니다.';
    } else {
      delete newErrors.password;
    }

    if (confirmPassword && value !== confirmPassword) {
      newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    } else {
      delete newErrors.confirmPassword;
    }

    setErrors(newErrors);
  };

  const handleConfirmPasswordChange = (value: string) => {
    setConfirmPassword(value);
    const newErrors = { ...errors };

    if (password && value !== password) {
      newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    } else {
      delete newErrors.confirmPassword;
    }

    setErrors(newErrors);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const newErrors: Record<string, string> = {};

    if (!email) newErrors.email = '이메일을 입력해주세요.';
    if (!password) newErrors.password = '비밀번호를 입력해주세요.';
    if (password.length < 8) newErrors.password = '비밀번호는 최소 8자 이상이어야 합니다.';
    if (password !== confirmPassword) newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    if (!name) newErrors.name = '이름을 입력해주세요.';

    setErrors(newErrors);
    if (Object.keys(newErrors).length > 0) return;

    setLoading(true);
    try {
      await AdminAuthService.signup({
        email,
        password,
        name,
      });
      
      alert('관리자 계정이 생성되었습니다!');
      router.push('/admin/login');
    } catch (err: any) {
      setErrors({ form: err.message || '회원가입에 실패했습니다.' });
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
        <h2 className="text-xl font-semibold mb-1 text-gray-900 dark:text-white">관리자 계정 만들기</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 text-center">
          관리자 권한으로 시스템을 관리할 수 있습니다.
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

        {/* 이름 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            이름
          </label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="관리자 이름"
            className={`w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white border rounded-lg px-3 py-2 placeholder:text-gray-400 dark:placeholder:text-gray-500 ${
              errors.name ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
            } focus:ring-2 focus:ring-red-500 dark:focus:ring-red-400`}
          />
          {errors.name && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.name}</p>}
        </div>

        {/* 비밀번호 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            비밀번호
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => handlePasswordChange(e.target.value)}
            placeholder="비밀번호를 입력하세요"
            className={`w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white border rounded-lg px-3 py-2 placeholder:text-gray-400 dark:placeholder:text-gray-500 ${
              errors.password ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
            } focus:ring-2 focus:ring-red-500 dark:focus:ring-red-400`}
          />
          {errors.password && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.password}</p>}
        </div>

        {/* 비밀번호 확인 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            비밀번호 확인
          </label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => handleConfirmPasswordChange(e.target.value)}
            placeholder="비밀번호를 다시 입력하세요"
            className={`w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white border rounded-lg px-3 py-2 placeholder:text-gray-400 dark:placeholder:text-gray-500 ${
              errors.confirmPassword ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
            } focus:ring-2 focus:ring-red-500 dark:focus:ring-red-400`}
          />
          {errors.confirmPassword && (
            <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.confirmPassword}</p>
          )}
        </div>

        {/* 에러 메시지 */}
        {errors.form && <p className="text-red-500 dark:text-red-400 text-sm">{errors.form}</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-red-600 dark:bg-red-500 text-white py-2 rounded-lg mt-4 hover:bg-red-700 dark:hover:bg-red-600 disabled:bg-gray-400 dark:disabled:bg-gray-600"
        >
          {loading ? '가입 중...' : '관리자 계정 만들기'}
        </button>

        <div className="text-center text-sm text-gray-600 dark:text-gray-400 mt-6">
          이미 관리자 계정이 있으신가요?{' '}
          <Link href="/admin/login" className="text-red-600 dark:text-red-400 hover:underline font-medium">
            관리자 로그인
          </Link>
        </div>
      </form>
    </div>
  );
};

export default AdminSignupForm;