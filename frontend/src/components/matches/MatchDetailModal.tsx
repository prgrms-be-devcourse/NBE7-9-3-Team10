'use client';

import { FC, useState, useEffect } from 'react';
import Modal from '../ui/Modal';
import Button from '../ui/Button';
import ReportModal from './ReportModal';
import { MatchService } from '../../lib/services/matchService';
import { getErrorMessage } from '../../lib/utils/helpers';
import type { MatchRecommendationDetailResponse } from '../../types/match';

interface MatchDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  match: MatchRecommendationDetailResponse | null;
  onLike?: (receiverId: number) => void;
  onCancelLike?: (receiverId: number) => void;
  isLiked?: boolean;
}

const MatchDetailModal: FC<MatchDetailModalProps> = ({ isOpen, onClose, match, onLike, onCancelLike, isLiked = false }) => {
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'info' | 'error' } | null>(null);
  const [isLiking, setIsLiking] = useState(false);
  const [currentLikeState, setCurrentLikeState] = useState(isLiked);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [isReportSubmitting, setIsReportSubmitting] = useState(false);

  // 토스트 자동 제거
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // isLiked prop이 변경되면 currentLikeState 업데이트
  useEffect(() => {
    setCurrentLikeState(isLiked);
  }, [isLiked]);

  // 신고하기 핸들러
  const handleReportSubmit = async (category: string, content: string) => {
    if (!match || !match.email) {
      setToast({ message: '신고할 사용자 정보를 찾을 수 없습니다.', type: 'error' });
      return;
    }
    
    setIsReportSubmitting(true);
    try {
      await MatchService.reportUser({
        reportedEmail: match.email,
        category,
        content,
      });
      
      setIsReportModalOpen(false);
      setToast({ message: '신고가 접수되었습니다. 검토 후 조치하겠습니다.', type: 'success' });
    } catch (error) {
      console.error('신고 실패:', error);
      setToast({ message: getErrorMessage(error), type: 'error' });
    } finally {
      setIsReportSubmitting(false);
    }
  };

  if (!match) return null;

  const handleLikeClick = async () => {
    if (isLiking) return;
    
    setIsLiking(true);
    try {
      if (currentLikeState) {
        // 좋아요 취소
        if (onCancelLike) {
          await onCancelLike(match.receiverId);
          setCurrentLikeState(false);
          setToast({ message: '좋아요를 취소했습니다.', type: 'info' });
        }
      } else {
        // 좋아요 보내기
        if (onLike) {
          await onLike(match.receiverId);
          setCurrentLikeState(true);
          setToast({ message: '좋아요를 보냈습니다! 💝', type: 'success' });
        }
      }
    } catch (error) {
      // 백엔드 메시지 우선 사용
      const backendMessage = getErrorMessage(error);
      setToast({ message: backendMessage, type: 'error' });
    } finally {
      setIsLiking(false);
    }
  };

  // 수면 시간 텍스트 변환 (백엔드 MatchFilterService와 일치)
  const getSleepTimeText = (sleepTime?: number) => {
    if (sleepTime === undefined || sleepTime === null) return '정보 없음';
    if (sleepTime === 5) return '22시 이전';
    if (sleepTime === 4) return '22시~00시';
    if (sleepTime === 3) return '00시~02시';
    if (sleepTime === 2) return '02시~04시';
    if (sleepTime === 1) return '04시 이후';
    return '정보 없음';
  };

  // 청소 빈도 텍스트 변환 (백엔드 MatchFilterService와 일치)
  const getCleaningText = (frequency?: number) => {
    if (frequency === undefined || frequency === null) return '정보 없음';
    if (frequency === 5) return '매일';
    if (frequency === 4) return '주 2~3회';
    if (frequency === 3) return '주 1회';
    if (frequency === 2) return '월 1~2회';
    if (frequency === 1) return '거의 안함';
    return '정보 없음';
  };

  // 위생 수준 텍스트 변환
  const getHygieneLevelText = (level?: number) => {
    if (!level) return '보통';
    if (level === 1) return '매우 관대';
    if (level === 2) return '관대';
    if (level === 3) return '보통';
    if (level === 4) return '예민';
    if (level === 5) return '매우 예민';
    return '보통';
  };

  // 소음 민감도 텍스트 변환
  const getNoiseSensitivityText = (level?: number) => {
    if (!level) return '보통';
    if (level === 1) return '매우 둔감';
    if (level === 2) return '둔감';
    if (level === 3) return '보통';
    if (level === 4) return '예민';
    if (level === 5) return '매우 예민';
    return '보통';
  };

  // 방문자 빈도 텍스트 변환
  const getGuestFrequencyText = (frequency?: number) => {
    if (!frequency) return '정보 없음';
    if (frequency === 1) return '절대 불가';
    if (frequency === 2) return '월 1회 미만';
    if (frequency === 3) return '월 1~2회';
    if (frequency === 4) return '주 1회';
    if (frequency === 5) return '매우 잦음';
    return '정보 없음';
  };

  // 음주 빈도 텍스트 변환
  const getDrinkingFrequencyText = (frequency?: number) => {
    if (!frequency) return '정보 없음';
    if (frequency === 1) return '전혀 안 마심';
    if (frequency === 2) return '밖에서만';
    if (frequency === 3) return '집에서 가끔';
    if (frequency === 4) return '집에서 주 3~4회';
    if (frequency === 5) return '집에서 매일';
    return '정보 없음';
  };

  return (
    <>
      {/* 토스트 메시지 */}
      {toast && (
        <div className="fixed top-20 right-6 z-[60] max-w-sm animate-slide-in-right">
          <div className={`rounded-lg p-4 shadow-2xl ${
            toast.type === 'success' ? 'bg-green-50 dark:bg-green-900/20 border-2 border-green-200 dark:border-green-800' :
            toast.type === 'info' ? 'bg-blue-50 dark:bg-blue-900/20 border-2 border-blue-200 dark:border-blue-800' :
            'bg-red-50 dark:bg-red-900/20 border-2 border-red-200 dark:border-red-800'
          }`}>
            <div className="flex items-start">
              <svg 
                className={`w-5 h-5 mr-3 flex-shrink-0 ${
                  toast.type === 'success' ? 'text-green-600 dark:text-green-400' :
                  toast.type === 'info' ? 'text-blue-600 dark:text-blue-400' :
                  'text-red-600 dark:text-red-400'
                }`}
                fill="currentColor" 
                viewBox="0 0 20 20"
              >
                {toast.type === 'success' ? (
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                ) : toast.type === 'info' ? (
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                ) : (
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                )}
              </svg>
              <p className={`text-sm font-medium ${
                toast.type === 'success' ? 'text-green-800 dark:text-green-300' :
                toast.type === 'info' ? 'text-blue-800 dark:text-blue-300' :
                'text-red-800 dark:text-red-300'
              }`}>
                {toast.message}
              </p>
            </div>
          </div>
        </div>
      )}

      <Modal isOpen={isOpen} onClose={onClose} title={`${match.name}님의 상세 정보`} size="md">
        <div className="space-y-5">
        {/* 간소화된 헤더 */}
        <div className="text-center pb-3 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-center gap-3 text-sm text-gray-600 dark:text-gray-400">
            <span className="flex items-center gap-1">
              🎓 {match.university}
            </span>
            <span className="text-gray-300">•</span>
            <span>{match.gender === 'MALE' ? '남학생' : '여학생'} · {match.age}세</span>
            <span className="text-gray-300">•</span>
            <span className="text-indigo-600 dark:text-indigo-400 font-semibold">매칭률 {Math.round((match.preferenceScore || 0) * 100)}%</span>
          </div>
        </div>

        {/* 주요 특징 */}
        <div>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-3">주요 특징</h3>
          <div className="flex flex-wrap gap-2">
            <span className="px-3 py-1.5 bg-indigo-50 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300 rounded-full text-sm font-semibold">
              {match.mbti}
            </span>
            <span className="px-3 py-1.5 bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300 rounded-full text-sm font-semibold">
              {match.isSmoker ? '흡연' : '비흡연'}
            </span>
            <span className="px-3 py-1.5 bg-green-50 text-green-700 dark:bg-green-900/30 dark:text-green-300 rounded-full text-sm font-semibold">
              {match.isPetAllowed ? '반려동물 가능' : '반려동물 불가'}
            </span>
            {match.isSnoring && (
              <span className="px-3 py-1.5 bg-orange-50 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300 rounded-full text-sm font-semibold">
                코골이
              </span>
            )}
          </div>
        </div>

        {/* 생활 패턴 - 2열 그리드 */}
        <div>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-3">생활 패턴</h3>
          <div className="grid grid-cols-2 gap-2.5">
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">취침 시간대</p>
              <p className="text-sm text-gray-900 dark:text-white font-semibold">{getSleepTimeText(match.sleepTime)}</p>
            </div>
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">청소 빈도</p>
              <p className="text-sm text-gray-900 dark:text-white font-semibold">{getCleaningText(match.cleaningFrequency)}</p>
            </div>
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">위생 수준</p>
              <p className="text-sm text-gray-900 dark:text-white font-semibold">{getHygieneLevelText(match.hygieneLevel)}</p>
            </div>
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">소음 민감도</p>
              <p className="text-sm text-gray-900 dark:text-white font-semibold">{getNoiseSensitivityText(match.noiseSensitivity)}</p>
            </div>
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">음주 빈도</p>
              <p className="text-sm text-gray-900 dark:text-white font-semibold">{getDrinkingFrequencyText(match.drinkingFrequency)}</p>
            </div>
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">손님 초대 빈도</p>
              <p className="text-sm text-gray-900 dark:text-white font-semibold">{getGuestFrequencyText(match.guestFrequency)}</p>
            </div>
          </div>
        </div>

        {/* 액션 버튼 */}
        <div className="flex gap-3 pt-2">
          {currentLikeState ? (
            <button
              onClick={handleLikeClick}
              disabled={isLiking}
              className="flex-1 py-3 rounded-lg font-medium transition-colors flex items-center justify-center gap-2 bg-red-600 dark:bg-red-500 text-white hover:bg-red-700 dark:hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLiking ? (
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
              disabled={isLiking}
              className="flex-1 py-3 rounded-lg font-medium transition-colors flex items-center justify-center gap-2 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLiking ? (
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
          
          <button
            onClick={() => setIsReportModalOpen(true)}
            className="px-6 py-3 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors flex items-center gap-2"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
              <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"></path>
              <path d="M12 9v4"></path>
              <path d="M12 17h.01"></path>
            </svg>
            신고
          </button>
        </div>
      </div>
    </Modal>
    
    {/* 신고 모달 */}
    <ReportModal
      isOpen={isReportModalOpen}
      onClose={() => setIsReportModalOpen(false)}
      onSubmit={handleReportSubmit}
      reportedUserName={match?.name || ''}
      isSubmitting={isReportSubmitting}
    />
    </>
  );
};

export default MatchDetailModal;
