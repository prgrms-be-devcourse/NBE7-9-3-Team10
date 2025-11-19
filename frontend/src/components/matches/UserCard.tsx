'use client';

import { useState, FC } from 'react';
import { MatchService } from '@/lib/services/matchService';
import { getErrorMessage } from '@/lib/utils/helpers';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card';
import Button from '@/components/ui/Button';

// TODO: 백엔드의 MatchRecommendation DTO에 맞춰 타입 정의 필요
interface UserCardProps {
  user: any; 
  onLikeChange: (userId: number, isLiked: boolean) => void;
  onViewDetail?: (receiverId: number) => void;
  appliedFilters?: any;
}

const UserCard: FC<UserCardProps> = ({ user, onLikeChange, onViewDetail }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const handleLikeClick = async () => {
    setIsLoading(true);
    try {
      if (user.isLiked) {
        await MatchService.cancelLike(user.receiverId);
        onLikeChange(user.receiverId, false);
        setToast({ message: '좋아요를 취소했습니다.', type: 'success' });
      } else {
        const response = await MatchService.sendLike(user.receiverId);
        onLikeChange(user.receiverId, true);
        
        // 상호 매칭 여부에 따라 다른 메시지 표시
        if (response.data?.data?.isMutual) {
          setToast({ message: '매칭이 성사되었습니다! 채팅방으로 이동합니다.', type: 'success' });
          // TODO: 채팅방으로 이동하는 로직 추가 (예: router.push)
        } else {
          setToast({ message: '좋아요를 보냈습니다! 💝', type: 'success' });
        }
      }
    } catch (error) {
      // 백엔드 메시지 우선 사용
      const errorMessage = getErrorMessage(error);
      setToast({ message: errorMessage, type: 'error' });
    } finally {
      setIsLoading(false);
      setTimeout(() => setToast(null), 3000);
    }
  };

  // 수면 시간 텍스트 변환 (상세보기와 통일)
  const getSleepTimeText = (sleepTime?: number) => {
    if (sleepTime === undefined || sleepTime === null) return null;
    if (sleepTime === 5) return '22시 이전';
    if (sleepTime === 4) return '22시~00시';
    if (sleepTime === 3) return '00시~02시';
    if (sleepTime === 2) return '02시~04시';
    if (sleepTime === 1) return '04시 이후';
    return null;
  };

  // 청소 빈도 텍스트 변환 (상세보기와 통일)
  const getCleaningText = (frequency?: number) => {
    if (frequency === undefined || frequency === null) return null;
    if (frequency === 5) return '매일';
    if (frequency === 4) return '주 2~3회';
    if (frequency === 3) return '주 1회';
    if (frequency === 2) return '월 1~2회';
    if (frequency === 1) return '거의 안함';
    return null;
  };

  return (
    <>
      {/* Toast 메시지 */}
      {toast && (
        <div className="fixed top-20 right-6 z-50 animate-fade-in">
          <div className={`px-6 py-3 rounded-lg shadow-lg ${
            toast.type === 'success' 
              ? 'bg-green-500 text-white' 
              : 'bg-red-500 text-white'
          }`}>
            {toast.message}
          </div>
        </div>
      )}

      <Card className="relative">
        {onViewDetail && (
          <button
            onClick={() => onViewDetail(user.receiverId)}
            className="absolute top-4 right-4 px-2.5 py-1 text-xs text-blue-600 dark:text-blue-400 bg-white dark:bg-gray-800 border border-blue-600 dark:border-blue-500 rounded-md hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors whitespace-nowrap z-10"
          >
            상세보기
          </button>
        )}
        <CardHeader>
          <div className="flex justify-between items-start pr-20">
            <div>
              <CardTitle>{user.name} <span className="text-base font-normal text-gray-500 dark:text-gray-400">{user.age}세</span></CardTitle>
              <CardDescription>{user.university}</CardDescription>
            </div>
            <div className="text-right">
              <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">{Math.round((user.preferenceScore || 0) * 100)}%</p>
              <p className="text-sm text-gray-500 dark:text-gray-400">매칭률</p>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {/* 기본 태그 */}
            <div className="flex flex-wrap gap-2">
              <span className="px-3 py-1 bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 text-xs font-semibold rounded-full">
                {user.gender === 'MALE' ? '남학생' : '여학생'}
              </span>
              <span className="px-3 py-1 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 text-xs font-semibold rounded-full">
                {user.isSmoker ? '🚬 흡연' : '🚭 비흡연'}
              </span>
              {user.mbti && (
                <span className="px-3 py-1 bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 text-xs font-semibold rounded-full">
                  {user.mbti}
                </span>
              )}
            </div>

            {/* 생활 패턴 정보 */}
            <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-100 dark:border-gray-700">
              {getSleepTimeText(user.sleepTime) && (
                <div className="flex items-center gap-1.5 text-xs">
                  <span className="text-gray-500 dark:text-gray-400">🌙</span>
                  <span className="text-gray-700 dark:text-gray-300 font-medium">{getSleepTimeText(user.sleepTime)}</span>
                </div>
              )}
              {getCleaningText(user.cleaningFrequency) && (
                <div className="flex items-center gap-1.5 text-xs">
                  <span className="text-gray-500 dark:text-gray-400">🧹</span>
                  <span className="text-gray-700 dark:text-gray-300 font-medium">{getCleaningText(user.cleaningFrequency)}</span>
                </div>
              )}
            </div>

            {/* 거주 기간 정보 */}
            {(user.startUseDate || user.endUseDate) && (
              <div className="flex items-center gap-1.5 text-xs pt-2 border-t border-gray-100 dark:border-gray-700">
                <span className="text-gray-500 dark:text-gray-400">룸셰어 기간</span>
                <span className="text-gray-700 dark:text-gray-300 font-medium">
                  {user.startUseDate && new Date(user.startUseDate).toLocaleDateString('ko-KR', { year: '2-digit', month: 'short', day: 'numeric' })}
                  {user.startUseDate && user.endUseDate && ' ~ '}
                  {user.endUseDate && new Date(user.endUseDate).toLocaleDateString('ko-KR', { year: '2-digit', month: 'short', day: 'numeric' })}
                </span>
              </div>
            )}
          </div>
        </CardContent>
        <CardFooter>
          {user.isLiked ? (
            <button
              onClick={handleLikeClick}
              disabled={isLoading}
              className="w-full py-3 rounded-lg font-medium transition-colors flex items-center justify-center gap-2 bg-red-500 dark:bg-red-600 text-white hover:bg-red-600 dark:hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  취소 중...
                </>
              ) : (
                <>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5 fill-current">
                    <path d="M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5"></path>
                  </svg>
                  좋아요 취소
                </>
              )}
            </button>
          ) : (
            <button
              onClick={handleLikeClick}
              disabled={isLoading}
              className="w-full py-3 rounded-lg font-medium transition-colors flex items-center justify-center gap-2 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  전송 중...
                </>
              ) : (
                <>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
                    <path d="M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5"></path>
                  </svg>
                  좋아요
                </>
              )}
            </button>
          )}
        </CardFooter>
      </Card>
    </>
  );
};

export default UserCard;
