'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { MatchStatusCard } from '../../../components/matches/MatchStatusCard';
import { Card } from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';
import { MatchService } from '../../../lib/services/matchService';
import { getErrorMessage } from '../../../lib/utils/helpers';
import type { MatchStatusResponse } from '../../../types/match';
import { CheckCircle, XCircle, MessageCircle } from 'lucide-react';
import AppHeader from '@/components/layout/AppHeader'; 

export default function MatchStatusPage() {
  const router = useRouter();
  const [matchStatus, setMatchStatus] = useState<MatchStatusResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [processingMatchId, setProcessingMatchId] = useState<number | null>(null);  

  // 토스트 자동 제거
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // 매칭 상태 조회
  const fetchMatchStatus = useCallback(async () => {
    try {
      setIsLoading(true);
      const response = await MatchService.getMatchStatus();
      const data = (response as any).data || response;
      setMatchStatus(data);
    } catch (error) {
      console.error('매칭 상태 조회 실패:', error);
      setToast({ message: getErrorMessage(error), type: 'error' });
    } finally {
      setIsLoading(false);
    }
  }, []);

  const handleConfirmMatch = async (matchId: number) => {
    if (!confirm('룸메이트 매칭을 확정하시겠습니까?')) {
      return;
    }

    try {
      setProcessingMatchId(matchId);
      await MatchService.confirmMatch(matchId);
      setToast({ message: '✅ 매칭이 확정되었습니다!', type: 'success' });
      await fetchMatchStatus(); // 상태 새로고침
    } catch (error) {
      console.error('매칭 확정 실패:', error);
      const errorMessage = getErrorMessage(error);
      if (errorMessage.includes('이미 응답')) {
        setToast({ message: '⚠️ 이미 확정 의사를 전달했습니다.', type: 'error' });
      } else {
        setToast({ message: errorMessage, type: 'error' });
      }
    } finally {
      setProcessingMatchId(null);
    }
  };

  const handleRejectMatch = async (matchId: number, partnerName: string) => {
    if (!confirm(`${partnerName}님과의 룸메이트 매칭을 거절하시겠습니까?\n거절 시 다시 되돌릴 수 없습니다.`)) {
      return;
    }

    try {
      setProcessingMatchId(matchId);
      await MatchService.rejectMatch(matchId);
      setToast({ message: '❌ 매칭을 거절했습니다.', type: 'success' });
      await fetchMatchStatus(); // 상태 새로고침
    } catch (error) {
      console.error('매칭 거절 실패:', error);
      const errorMessage = getErrorMessage(error);
      if (errorMessage.includes('이미 응답')) {
        setToast({ message: '⚠️ 이미 거절 의사를 전달했습니다.', type: 'error' });
      } else {
        setToast({ message: errorMessage, type: 'error' });
      }
    } finally {
      setProcessingMatchId(null);
    }
  };

  const handleGoToChat = () => {
    router.push('/chat');
  };

  // 새로고침
  const handleRefresh = () => {
    fetchMatchStatus();
  };

  // 추천 목록으로 이동
  const handleViewRecommendations = () => {
    router.push('/matches');
  };

  // 결과 보기
  const handleViewResults = () => {
    router.push('/matches/results');
  };

  useEffect(() => {
    fetchMatchStatus();
  }, [fetchMatchStatus]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (!matchStatus) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="text-center py-12">
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
              매칭 상태를 불러올 수 없습니다
            </h1>
            <p className="text-gray-500 dark:text-gray-400 mb-6">
              잠시 후 다시 시도해주세요.
            </p>
            <Button onClick={handleRefresh}>
              다시 시도
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <AppHeader />  {/* ✅ 추가 */}
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* 헤더 */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
              매칭 상태
            </h1>
            <p className="text-gray-600 dark:text-gray-400">
              현재 매칭 상태와 진행 상황을 확인하세요
            </p>
          </div>
          <Button variant="outline" onClick={handleRefresh}>
            새로고침
          </Button>
        </div>

        {/* 매칭 상태 카드 */}
        <div className="mb-8">
          <MatchStatusCard
            status={matchStatus}
            onViewResults={handleViewResults}
            onViewMatches={handleViewRecommendations}
          />
        </div>

        {/* 매칭 목록 */}
        {matchStatus.matches && matchStatus.matches.length > 0 ? (
          <div className="space-y-4 mb-8">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
              매칭 목록
            </h2>
            {matchStatus.matches.map((match) => {
              const myResponse = match.myResponse;
              const partnerResponse = match.partnerResponse;
              const isWaitingForPartner = match.waitingForPartner;
              const canRespond = match.matchStatus === 'PENDING' && myResponse === 'PENDING';
              const isProcessing = processingMatchId === match.id;

              return (
                <Card key={match.id} className="p-6 hover:shadow-lg transition-shadow">
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h3 className="text-lg font-medium text-gray-900 dark:text-white">
                        {match.partner.name}
                      </h3>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {match.partner.university}
                      </p>
                    </div>
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      match.matchStatus === 'PENDING' ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400' :
                      match.matchStatus === 'ACCEPTED' ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400' :
                      'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
                    }`}>
                      {match.matchStatus === 'PENDING' && '대기 중'}
                      {match.matchStatus === 'ACCEPTED' && '수락됨'}
                      {match.matchStatus === 'REJECTED' && '거절됨'}
                    </span>
                  </div>

                  {/* 양방향 응답 상태 표시 */}
                  <div className="mb-4 space-y-2">
                    <div className="flex items-center gap-4 text-sm">
                      <div className="flex items-center gap-2">
                        <span className="text-gray-600 dark:text-gray-400">내 응답:</span>
                        <span className={`px-2 py-1 rounded text-xs font-medium ${
                          myResponse === 'ACCEPTED' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' :
                          myResponse === 'REJECTED' ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' :
                          'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                        }`}>
                          {myResponse === 'PENDING' && '대기 중'}
                          {myResponse === 'ACCEPTED' && '수락함'}
                          {myResponse === 'REJECTED' && '거절함'}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-gray-600 dark:text-gray-400">상대방 응답:</span>
                        <span className={`px-2 py-1 rounded text-xs font-medium ${
                          partnerResponse === 'ACCEPTED' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' :
                          partnerResponse === 'REJECTED' ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' :
                          'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                        }`}>
                          {partnerResponse === 'PENDING' && '대기 중'}
                          {partnerResponse === 'ACCEPTED' && '수락함'}
                          {partnerResponse === 'REJECTED' && '거절함'}
                        </span>
                      </div>
                    </div>

                    {/* 상대방 응답 대기 중 표시 */}
                    {isWaitingForPartner && (
                      <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-3">
                        <p className="text-sm text-blue-700 dark:text-blue-300 font-medium">
                          ⏰ 상대방의 응답을 기다리고 있습니다
                        </p>
                      </div>
                    )}

                    {/* 상대방이 수락한 경우 안내 */}
                    {partnerResponse === 'ACCEPTED' && myResponse === 'PENDING' && (
                      <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-3">
                        <p className="text-sm text-green-700 dark:text-green-300 font-medium">
                          ✅ 상대방이 수락했습니다. 아래에서 확정하거나 거절할 수 있습니다.
                        </p>
                      </div>
                    )}

                    {/* 상대방이 거절한 경우 안내 */}
                    {partnerResponse === 'REJECTED' && (
                      <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3">
                        <p className="text-sm text-red-700 dark:text-red-300 font-medium">
                          ❌ 상대방이 거절했습니다
                        </p>
                      </div>
                    )}
                  </div>

                  <div className="space-y-2 mb-4">
                    <div className="text-sm text-gray-600 dark:text-gray-400">
                      {match.matchType === 'LIKE' ? '💝 좋아요' : '✨ 정식 룸메 신청'}
                    </div>
                    {match.message && (
                      <p className="text-sm text-gray-500 dark:text-gray-400 italic">
                        {match.message}
                      </p>
                    )}
                    <div className="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400">
                      <span>매칭률: {Math.round((match.preferenceScore || 0) * 100)}%</span>
                      <span>•</span>
                      <span>
                        {new Date(match.createdAt).toLocaleDateString('ko-KR', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        })}
                      </span>
                    </div>
                  </div>

                  {/* 액션 버튼 영역 */}
                  <div className="flex gap-2 pt-4 border-t border-gray-200 dark:border-gray-700">
                    {canRespond && (
                      <>
                        <Button
                          variant="outline"
                          onClick={() => handleRejectMatch(match.id, match.partner.name)}
                          disabled={isProcessing}
                          className="flex-1 flex items-center justify-center"
                        >
                          <XCircle className="w-4 h-4 mr-2" />
                          <span>{isProcessing ? '처리 중...' : '거절'}</span>
                        </Button>
                        <Button
                          onClick={() => handleConfirmMatch(match.id)}
                          disabled={isProcessing}
                          className="flex-1 flex items-center justify-center"
                        >
                          <CheckCircle className="w-4 h-4 mr-2" />
                          <span>{isProcessing ? '처리 중...' : '확정'}</span>
                        </Button>
                      </>
                    )}
                    {match.matchStatus === 'ACCEPTED' && (
                      <Button
                        onClick={handleGoToChat}
                        className="flex-1 flex items-center justify-center"
                      >
                        <MessageCircle className="w-4 h-4 mr-2" />
                        <span>채팅하기</span>
                      </Button>
                    )}
                    {!canRespond && match.matchStatus === 'PENDING' && (
                      <div className="flex-1 text-sm text-gray-500 dark:text-gray-400 text-center py-2">
                        상대방의 응답을 기다리는 중입니다
                      </div>
                    )}
                  </div>
                </Card>
              );
            })}
          </div>
        ) : (
          // 빈 매칭 목록 UI 추가
          <div className="flex flex-col items-center justify-center py-20 mb-8">
            <div className="w-32 h-32 bg-gradient-to-br from-blue-100 to-indigo-100 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-full flex items-center justify-center mb-6">
              <svg className="w-16 h-16 text-blue-400 dark:text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            <h3 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
              아직 매칭이 없습니다
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-8 text-center max-w-md">
              추천 목록에서 마음에 드는 룸메이트를 찾아<br />
              좋아요를 보내고 대화를 시작해보세요!
            </p>
            <Button onClick={handleViewRecommendations}>
              룸메이트 찾으러 가기
            </Button>
          </div>
        )}

        {/* 액션 버튼 */}
        <div className="flex gap-4 justify-center">
          <Button onClick={handleViewRecommendations}>
            새 매칭 찾기
          </Button>
          <Button variant="outline" onClick={handleViewResults}>
            결과 보기
          </Button>
        </div>

        {/* 토스트 */}
        {toast && (
          <div className="fixed top-4 right-4 z-50">
            <div className={`max-w-sm w-full border rounded-lg shadow-lg p-4 ${
              toast.type === 'success' 
                ? 'bg-green-50 border-green-200 text-green-800' 
                : 'bg-red-50 border-red-200 text-red-800'
            }`}>
              <div className="flex items-start">
                <div className="flex-shrink-0 mr-3">
                  <span className="text-lg">{toast.type === 'success' ? '✅' : '❌'}</span>
                </div>
                <div className="flex-1">
                  <p className="text-sm whitespace-pre-line">{toast.message}</p> 
                </div>
                <button
                  onClick={() => setToast(null)}
                  className="flex-shrink-0 ml-2 text-gray-400 hover:text-gray-600"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

