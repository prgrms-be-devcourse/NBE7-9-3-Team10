'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Gender } from '@/types/user';
import { RegisterService } from '@/lib/services/registerService';
import { ApiError, ERROR_CODES } from '@/types/api';
import Link from "next/link";

interface SchoolInfo {
  schoolName: string;
  domain: string;
}

const RegisterForm = () => {
  const router = useRouter();

  // ===== 학교 관련 State =====
  const [university, setUniversity] = useState('');
  const [allSchools, setAllSchools] = useState<SchoolInfo[]>([]);
  const [filteredSchools, setFilteredSchools] = useState<SchoolInfo[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [schoolsLoading, setSchoolsLoading] = useState(true);

  // ===== 이메일 관련 State =====
  const [emailDomain, setEmailDomain] = useState('');
  const [emailId, setEmailId] = useState('');
  const [isSchoolVerified, setIsSchoolVerified] = useState(false);

  // ===== 인증 관련 State =====
  const [code, setCode] = useState('');
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [isVerified, setIsVerified] = useState(false);

  // ===== 사용자 정보 State =====
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [name, setName] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [gender, setGender] = useState<Gender | ''>('');
  const [agree, setAgree] = useState(false);

  // ===== UI State =====
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  // ===== useEffect: 학교 목록 초기화 =====
  useEffect(() => {
    const loadSchools = async () => {
      try {
        setSchoolsLoading(true);
        const schools = await RegisterService.getAllSchools();
        setAllSchools(schools);
        setFilteredSchools(schools);
      } catch (err) {
        console.error('학교 목록 로드 오류:', err);
        setErrors({ university: '학교 목록을 불러올 수 없습니다.' });
      } finally {
        setSchoolsLoading(false);
      }
    };

    loadSchools();
  }, []);

  // ===== 학교명 입력 시 실시간 필터링 =====
  const handleUniversityChange = (value: string) => {
    setUniversity(value);
    setIsSchoolVerified(false);
    setEmailDomain('');
    setEmailId('');
    setIsCodeSent(false);
    setCode('');
    setIsVerified(false);
    setShowDropdown(true);
    setErrors(prev => {
      const newErrors = { ...prev };
      delete newErrors.university;
      return newErrors;
    });

    // 입력값이 없으면 전체 목록 표시
    if (value === '') {
      setFilteredSchools(allSchools);
    } else {
      // 입력값 포함 학교만 필터링
      const filtered = allSchools.filter(school =>
        school.schoolName.includes(value)
      );
      setFilteredSchools(filtered);
    }
  };

  // ===== 드롭다운에서 학교 선택 =====
  const handleSelectSchool = (school: SchoolInfo) => {
    setUniversity(school.schoolName);
    setEmailDomain(school.domain);
    setIsSchoolVerified(true);
    setShowDropdown(false);
    setErrors(prev => {
      const newErrors = { ...prev };
      delete newErrors.university;
      return newErrors;
    });
    setMessage('학교가 확인되었습니다. 이메일을 입력해주세요.');
  };

  // ===== 인증코드 발송 =====
  const handleSendCode = async () => {
    if (!emailId) {
      setErrors({ ...errors, emailId: '이메일 아이디를 입력해주세요.' });
      return;
    }

    const fullEmail = `${emailId}@${emailDomain}`;

    setLoading(true);
    setErrors(prev => {
      const newErrors = { ...prev };
      delete newErrors.emailId;
      return newErrors;
    });
    setMessage('');

    try {
      await RegisterService.requestVerification(fullEmail);
      setIsCodeSent(true);
      setMessage('인증번호가 이메일로 전송되었습니다.');
    } catch (err) {
      const apiError = err as ApiError;
      setErrors(prev => ({ ...prev, emailId: apiError.message }));
    } finally {
      setLoading(false);
    }
  };

  // ===== 인증코드 입력 (숫자만) =====
  const handleCodeInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/[^0-9]/g, '');
    setCode(value);
  };

  // ===== 인증코드 검증 =====
  const handleVerifyCode = async () => {
    if (!code) {
      setErrors(prev => ({ ...prev, code: '인증번호를 입력해주세요.' }));
      return;
    }
    if (code.length !== 6) {
      setErrors(prev => ({ ...prev, code: '인증번호는 6자리여야 합니다.' }));
      return;
    }

    const fullEmail = `${emailId}@${emailDomain}`;

    setLoading(true);
    try {
      await RegisterService.verifyEmailCode(fullEmail, code);
      setIsVerified(true);
      setMessage('이메일 인증이 완료되었습니다');
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors.code;
        return newErrors;
      });
    } catch (err) {
      const apiError = err as ApiError;
      setErrors(prev => ({ ...prev, code: apiError.message }));
    } finally {
      setLoading(false);
    }
  };

  // ===== 비밀번호 변경 (유효성 검사 포함) =====
  const handlePasswordChange = (value: string) => {
    setPassword(value);
    const newErrors = { ...errors };
    if (value.length < 8)
      newErrors.password = '비밀번호는 최소 8자 이상이어야 합니다.';
    else delete newErrors.password;

    if (confirmPassword && value !== confirmPassword)
      newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    else delete newErrors.confirmPassword;

    setErrors(newErrors);
  };

  // ===== 비밀번호 확인 변경 =====
  const handleConfirmPasswordChange = (value: string) => {
    setConfirmPassword(value);
    const newErrors = { ...errors };

    if (password && value !== password)
      newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    else delete newErrors.confirmPassword;

    setErrors(newErrors);
  };

  // ===== 회원가입 제출 =====
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const newErrors: Record<string, string> = {};
    if (!isVerified) newErrors.email = '이메일 인증을 완료해주세요.';
    if (!password) newErrors.password = '비밀번호를 입력해주세요.';
    if (password.length < 8)
      newErrors.password = '비밀번호는 최소 8자 이상이어야 합니다.';
    if (password !== confirmPassword)
      newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    if (!name) newErrors.name = '이름을 입력해주세요.';
    if (!birthDate) newErrors.birthDate = '생년월일을 입력해주세요.';
    if (!gender) newErrors.gender = '성별을 선택해주세요.';
    if (!agree) newErrors.agree = '이용약관에 동의해주세요.';

    setErrors(newErrors);
    if (Object.keys(newErrors).length > 0) return;

    setLoading(true);
    const fullEmail = `${emailId}@${emailDomain}`;

    try {
      await RegisterService.signup({
        email: fullEmail,
        password,
        name,
        gender: gender as Gender,
        birthDate,
        university,
      });
      alert('회원가입이 완료되었습니다!');
      router.push('/login');
    } catch (err) {
      const apiError = err as ApiError;

      if (apiError.errorCode === ERROR_CODES.BAD_REQUEST) {
        if (apiError.message.includes('이메일')) {
          setErrors({ email: apiError.message });
          setIsVerified(false);
        } else {
          setErrors({ form: apiError.message });
        }
      } else if (apiError.errorCode === ERROR_CODES.CONFLICT) {
        setErrors({ email: apiError.message });
        setIsVerified(false);
      } else {
        setErrors({ form: apiError.message });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md bg-white rounded-2xl shadow p-8">
      <div className="flex flex-col items-center mb-6">
        <div className="bg-blue-600 text-white w-12 h-12 rounded-full flex items-center justify-center font-bold text-xl mb-3">
          U
        </div>
        <h2 className="text-xl font-semibold mb-1">계정 만들기</h2>
        <p className="text-sm text-gray-500 text-center">
          UniMate에 가입하여 완벽한 룸메이트를 찾아보세요.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* ===== 대학교 (자동완성) ===== */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">대학교</label>
          <div className="relative">
            <input
              type="text"
              value={university}
              onChange={(e) => handleUniversityChange(e.target.value)}
              onFocus={() => {
                if (!isSchoolVerified && !schoolsLoading) {
                  setShowDropdown(true);
                }
              }}
              placeholder="학교명을 입력하세요"
              disabled={isSchoolVerified || schoolsLoading}
              className={`w-full border rounded-lg px-3 py-2 placeholder:text-gray-400 ${
                errors.university ? 'border-red-500' : 'border-gray-300'
              } focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100`}
            />

            {/* 드롭다운 목록 */}
            {showDropdown && !isSchoolVerified && filteredSchools.length > 0 && (
              <div className="absolute top-full left-0 right-0 bg-white border border-gray-300 rounded-lg mt-1 max-h-48 overflow-y-auto z-10 shadow-lg">
                {filteredSchools.slice(0, 100).map((school, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSelectSchool(school)}
                    className="w-full text-left px-3 py-2 hover:bg-blue-100 text-sm border-b border-gray-100 last:border-b-0 transition-colors"
                  >
                    {school.schoolName}
                  </button>
                ))}
              </div>
            )}

            {/* 검색 결과 없음 */}
            {showDropdown && !isSchoolVerified && university && filteredSchools.length === 0 && (
              <div className="absolute top-full left-0 right-0 bg-white border border-gray-300 rounded-lg mt-1 z-10 shadow-lg">
                <div className="px-3 py-4 text-center text-gray-500 text-sm">
                  검색 결과가 없습니다.
                </div>
              </div>
            )}

            {/* 확인됨 표시 */}
            {isSchoolVerified && (
              <div className="absolute right-3 top-1/2 -translate-y-1/2">
                <span className="bg-green-500 text-white px-2 py-1 rounded text-xs font-medium">
                  확인됨
                </span>
              </div>
            )}
          </div>
          {errors.university && <p className="text-red-500 text-sm mt-1">{errors.university}</p>}
        </div>

        {/* ===== 이메일 ===== */}
        {isSchoolVerified && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
            <div className="flex space-x-2">
              <div className="flex-1 flex border border-gray-300 rounded-lg overflow-hidden">
                <input
                  type="text"
                  value={emailId}
                  onChange={(e) => setEmailId(e.target.value)}
                  placeholder="이메일 아이디"
                  disabled={isVerified}
                  className={`flex-1 px-3 py-2 placeholder:text-gray-400 text-sm focus:outline-none disabled:bg-gray-100 ${
                    errors.emailId ? 'border-red-500' : ''
                  }`}
                />
                <span className="px-2 py-2 bg-gray-100 text-gray-600 text-sm font-medium border-l border-gray-300">
                  @{emailDomain}
                </span>
              </div>
              <button
                type="button"
                onClick={handleSendCode}
                disabled={loading || isVerified}
                className={`px-3 py-2 rounded-lg text-white text-sm font-medium whitespace-nowrap ${
                  isVerified
                    ? 'bg-green-500 cursor-default'
                    : 'bg-blue-600 hover:bg-blue-700'
                } disabled:opacity-50`}
              >
                {isCodeSent ? '재전송' : '전송'}
              </button>
            </div>
            {errors.emailId && <p className="text-red-500 text-sm mt-1">{errors.emailId}</p>}
          </div>
        )}

        {/* ===== 인증번호 ===== */}
        {isCodeSent && !isVerified && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">인증번호</label>
            <div className="flex space-x-2">
              <input
                type="text"
                value={code}
                onChange={handleCodeInput}
                placeholder="인증번호 6자리"
                maxLength={6}
                className={`flex-1 border rounded-lg px-3 py-2 placeholder:text-gray-400 text-sm ${
                  errors.code ? 'border-red-500' : 'border-gray-300'
                } focus:ring-2 focus:ring-blue-500`}
              />
              <button
                type="button"
                onClick={handleVerifyCode}
                disabled={loading}
                className="bg-blue-600 text-white px-3 py-2 rounded-lg hover:bg-blue-700 text-sm font-medium whitespace-nowrap disabled:opacity-50"
              >
                확인
              </button>
            </div>
            {errors.code && <p className="text-red-500 text-sm mt-1">{errors.code}</p>}
          </div>
        )}

        {/* ===== 비밀번호 ===== */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => handlePasswordChange(e.target.value)}
            placeholder="비밀번호를 입력하세요"
            className={`w-full border rounded-lg px-3 py-2 placeholder:text-gray-400 text-sm ${
              errors.password ? 'border-red-500' : 'border-gray-300'
            } focus:ring-2 focus:ring-blue-500`}
          />
          {errors.password && <p className="text-red-500 text-sm mt-1">{errors.password}</p>}
        </div>

        {/* ===== 비밀번호 확인 ===== */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호 확인</label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => handleConfirmPasswordChange(e.target.value)}
            placeholder="비밀번호를 다시 입력하세요"
            className={`w-full border rounded-lg px-3 py-2 placeholder:text-gray-400 text-sm ${
              errors.confirmPassword ? 'border-red-500' : 'border-gray-300'
            } focus:ring-2 focus:ring-blue-500`}
          />
          {errors.confirmPassword && (
            <p className="text-red-500 text-sm mt-1">{errors.confirmPassword}</p>
          )}
        </div>

        {/* ===== 이름 ===== */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">이름</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="홍길동"
            className={`w-full border rounded-lg px-3 py-2 placeholder:text-gray-400 text-sm ${
              errors.name ? 'border-red-500' : 'border-gray-300'
            } focus:ring-2 focus:ring-blue-500`}
          />
          {errors.name && <p className="text-red-500 text-sm mt-1">{errors.name}</p>}
        </div>

        {/* ===== 생년월일 ===== */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">생년월일</label>
          <input
            type="date"
            value={birthDate}
            max={new Date().toISOString().split('T')[0]}
            onChange={(e) => setBirthDate(e.target.value)}
            className={`w-full border rounded-lg px-3 py-2 text-sm ${
              errors.birthDate ? 'border-red-500' : 'border-gray-300'
            } focus:ring-2 focus:ring-blue-500`}
          />
          {errors.birthDate && (
            <p className="text-red-500 text-sm mt-1">{errors.birthDate}</p>
          )}
        </div>

        {/* ===== 성별 ===== */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">성별</label>
          <div className="flex space-x-4">
            <label className="flex items-center space-x-2 text-sm">
              <input
                type="radio"
                name="gender"
                value="MALE"
                checked={gender === 'MALE'}
                onChange={() => setGender(Gender.MALE)}
              />
              <span>남성</span>
            </label>
            <label className="flex items-center space-x-2 text-sm">
              <input
                type="radio"
                name="gender"
                value="FEMALE"
                checked={gender === 'FEMALE'}
                onChange={() => setGender(Gender.FEMALE)}
              />
              <span>여성</span>
            </label>
          </div>
          {errors.gender && <p className="text-red-500 text-sm mt-1">{errors.gender}</p>}
        </div>

        {/* ===== 이용약관 ===== */}
        <div className="flex items-center space-x-2 text-sm">
          <input
            type="checkbox"
            checked={agree}
            onChange={(e) => setAgree(e.target.checked)}
          />
          <label className="text-gray-600">
            이용약관 및 개인정보처리방침에 동의합니다
          </label>
        </div>
        {errors.agree && <p className="text-red-500 text-sm">{errors.agree}</p>}

        {/* ===== 성공/에러 메시지 ===== */}
        {message && <p className="text-blue-600 text-sm">{message}</p>}
        {errors.form && <p className="text-red-500 text-sm">{errors.form}</p>}

        {/* ===== 제출 버튼 ===== */}
        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 text-white py-2 rounded-lg mt-4 hover:bg-blue-700 disabled:bg-gray-400 font-medium text-sm"
        >
          {loading ? '가입 중...' : '다음'}
        </button>

        {/* ===== 로그인 링크 ===== */}
        <div className="text-center text-xs text-gray-600 mt-4">
          이미 계정이 있으신가요?{" "}
          <Link
            href="/login"
            className="text-blue-600 hover:underline font-medium"
          >
            로그인
          </Link>
        </div>
      </form>
    </div>
  );
};

export default RegisterForm;