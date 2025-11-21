'use client';

import { useState, useEffect, useCallback } from 'react';
import { ProfileService } from '@/lib/services/profileService';
import { MatchService } from '@/lib/services/matchService';
import { getErrorMessage } from '@/lib/utils/helpers';
import UserCard from '@/components/matches/UserCard';
import MatchDetailModal from '@/components/matches/MatchDetailModal';
import LoadingSpinner from '@/components/ui/LoadingSpinner';
import Button from '@/components/ui/Button';
import MatchPreferenceModal from '@/components/profile/MatchPreferenceModal';
import ConfirmModal from '@/components/ui/ConfirmModal';
import { useRouter } from 'next/navigation';
import type { MatchRecommendationDetailResponse } from '@/types/match';

type RecommendedUser = any;

interface MatchFilters {
  sleepPattern?: string;
  cleaningFrequency?: string;
  ageRange?: string;
  startDate?: string;
  endDate?: string;
}

const PreferencePrompt = ({ onOpenModal }: { onOpenModal: () => void }) => (
  <div className="flex flex-col items-center justify-center h-[60vh] text-center">
    <div className="bg-purple-100 dark:bg-purple-900/20 rounded-full p-6 mb-6">
      <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 text-purple-600 dark:text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
    </div>
    <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">매칭 선호도 등록</h2>
    <p className="text-gray-600 dark:text-gray-300 mb-6 max-w-md">
      당신의 생활 패턴과 선호도를 등록하면 더욱 정확한 룸메이트 추천을 받을 수 있습니다.
    </p>
    <Button onClick={onOpenModal} size="lg">선호도 등록하기</Button>
  </div>
);

import AppHeader from '@/components/layout/AppHeader';

export default function MatchesPage() {
  const [users, setUsers] = useState<RecommendedUser[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<RecommendedUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [hasPreferences, setHasPreferences] = useState(false);
  const [isPreferenceModalOpen, setIsPreferenceModalOpen] = useState(false);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<MatchRecommendationDetailResponse | null>(null);
  const [filters, setFilters] = useState<MatchFilters>({});
  const router = useRouter();

  const [likedIds, setLikedIds] = useState<Set<number>>(new Set());

  useEffect(() => {
    // 사용자 변경 시 좋아요 상태 초기화
    setLikedIds(new Set());
    
    const checkProfileStatus = async () => {
      setIsLoading(true);
      try {
        const response = await ProfileService.getMyProfile();
        const profile = (response as any).data || response;
        
        if (profile && profile.matchingEnabled) {
          setHasPreferences(true);
        } else {
          setHasPreferences(false);
        }
      } catch (err) {
        const apiError = err as any;
        // 404는 프로필이 없는 정상적인 케이스
        if (apiError?.response?.status === 404 || apiError?.status === 404) {
          setHasPreferences(false);
        } else {
          // 백엔드 에러 메시지 우선 사용
          setError(getErrorMessage(err));
        }
      } finally {
        setIsLoading(false);
      }
    };

    checkProfileStatus();
  }, []);

  useEffect(() => {
    if (hasPreferences) {
      const fetchRecommendations = async () => {
        setIsLoading(true);
        try {
          const response = await MatchService.getRecommendations({});
          const rawData = (response as any).data?.data || (response as any).data || response;

          const recommendations = (rawData.recommendations || []).map((user: any) => ({
            ...user,
            // 서버 상태 우선, 없으면 로컬 상태 사용
            isLiked: (user.matchType === 'LIKE' && user.matchStatus === 'PENDING') || likedIds.has(user.receiverId),
          }));
          
          setUsers(recommendations);
          setFilteredUsers(recommendations);
        } catch (err) {
          console.error('❌ 필터 적용 실패:', err);
          if (err && typeof err === 'object') {
            console.error('에러 상세:', {
              message: (err as any).message,
              status: (err as any).status,
              response: (err as any).response,
              data: (err as any).response?.data,
            });
          }
          setError(getErrorMessage(err));
        } finally {
          setIsLoading(false);
        }
      };
      fetchRecommendations();
    }
  }, [hasPreferences]);

  const handleLikeChange = (receiverId: number, isLiked: boolean) => {
    setLikedIds(prevIds => {
      const newIds = new Set(prevIds);
      if (isLiked) {
        newIds.add(receiverId);
      } else {
        newIds.delete(receiverId);
      }
      return newIds;
    });

    const updateUser = (user: RecommendedUser) => 
      user.receiverId === receiverId 
        ? { ...user, isLiked, matchType: isLiked ? 'LIKE' : undefined, matchStatus: isLiked ? 'PENDING' : undefined } 
        : user;

    setUsers(currentUsers => currentUsers.map(updateUser));
    setFilteredUsers(currentUsers => currentUsers.map(updateUser));
  };

  const handleOpenPreferenceModal = () => setIsPreferenceModalOpen(true);
  const handleClosePreferenceModal = useCallback(() => setIsPreferenceModalOpen(false), []);
  
  const openCancelModal = () => setIsCancelModalOpen(true);
  const closeCancelModal = () => setIsCancelModalOpen(false);

  const handleCancelMatching = async () => {
    try {
      await MatchService.cancelMatching();
      setHasPreferences(false);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      closeCancelModal();
    }
  };

  const handlePreferenceSave = () => {
    setHasPreferences(true);
    handleClosePreferenceModal();
  };

  const handleApplyFilters = async (newFilters: MatchFilters) => {
    setIsLoading(true);
    try {
      // 빈 문자열을 undefined로 변환 (백엔드 validation 통과를 위해)
      const cleanedFilters = {
        sleepPattern: newFilters.sleepPattern || undefined,
        ageRange: newFilters.ageRange || undefined,
        cleaningFrequency: newFilters.cleaningFrequency || undefined,
        // startDate: newFilters.startDate || undefined,
        // endDate: newFilters.endDate || undefined,
      };
      
      const response = await MatchService.getRecommendations(cleanedFilters);
      const rawData = (response as any).data?.data || (response as any).data || response;
      const recommendations = rawData.recommendations || [];
      
      setUsers(recommendations);
      setFilteredUsers(recommendations);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetFilters = () => {
    setFilters({});
    handleApplyFilters({});
  };

  const handleViewResults = () => {
    router.push('/matches/results');
  };

  const handleViewDetail = async (receiverId: number) => {
    setIsDetailLoading(true);
    setIsDetailModalOpen(false);
    try {
      const detail = await MatchService.getMatchDetail(receiverId);
      setSelectedUser(detail);
      setIsDetailLoading(false);
      setIsDetailModalOpen(true);
    } catch (err) {
      console.error('상세 정보 조회 실패:', err);
      setError(getErrorMessage(err));
      setIsDetailLoading(false);
    }
  };

  const handleCloseDetailModal = () => {
    setIsDetailModalOpen(false);
    setSelectedUser(null);
  };

  const handleLikeFromModal = async (receiverId: number) => {
    try {
      await MatchService.sendLike(receiverId);
      setUsers(currentUsers =>
        currentUsers.map(user =>
          user.receiverId === receiverId ? { ...user, isLiked: true, matchType: 'LIKE', matchStatus: 'PENDING' } : user
        )
      );
      setFilteredUsers(currentUsers =>
        currentUsers.map(user =>
          user.receiverId === receiverId ? { ...user, isLiked: true, matchType: 'LIKE', matchStatus: 'PENDING' } : user
        )
      );
    } catch (err) {
      console.error('좋아요 전송 실패:', err);
      throw err; // MatchDetailModal에서 처리하도록 에러 전달
    }
  };

  const handleCancelLikeFromModal = async (receiverId: number) => {
    try {
      await MatchService.cancelLike(receiverId);
      setUsers(currentUsers =>
        currentUsers.map(user =>
          user.receiverId === receiverId ? { ...user, isLiked: false, matchType: undefined, matchStatus: undefined } : user
        )
      );
      setFilteredUsers(currentUsers =>
        currentUsers.map(user =>
          user.receiverId === receiverId ? { ...user, isLiked: false, matchType: undefined, matchStatus: undefined } : user
        )
      );
    } catch (err) {
      console.error('좋아요 취소 실패:', err);
      throw err; // MatchDetailModal에서 처리하도록 에러 전달
    }
  };

  return (
    <>
      <AppHeader />
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-4 sm:p-6 lg:p-8">
        <div className="max-w-6xl mx-auto">
          {isLoading ? (
            <LoadingSpinner />
        ) : error ? (
          <div className="flex flex-col items-center justify-center min-h-[60vh]">
            <div className="bg-red-50 dark:bg-red-900/20 rounded-full p-6 mb-6">
              <svg className="w-16 h-16 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">오류가 발생했습니다</h2>
            <p className="text-gray-600 dark:text-gray-300 mb-6">{error}</p>
            <Button onClick={() => window.location.reload()}>다시 시도</Button>
          </div>
        ) : hasPreferences ? (
              <>
                {/* 메인 컨텐츠 - 필터(왼쪽) + 그리드(오른쪽) */}
                <div className="flex gap-6">
                  {/* 왼쪽 사이드바 필터 */}
                  <div className="w-64 flex-shrink-0">
                    <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm sticky top-4">
                      {/* 필터 헤더 */}
                      <div className="flex items-center justify-between mb-6">
                        <h3 className="text-lg font-bold text-gray-900 dark:text-white">필터</h3>
                        <button
                          onClick={handleResetFilters}
                          className="text-sm text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 font-medium"
                        >
                          초기화
                        </button>
                      </div>

                      <div className="space-y-5">
                        {/* 수면 패턴 */}
                        <div>
                          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                            수면 패턴
                          </label>
                          <select
                            value={filters.sleepPattern || ''}
                            onChange={(e) => {
                              const newFilters = { ...filters, sleepPattern: e.target.value || undefined };
                              setFilters(newFilters);
                              handleApplyFilters(newFilters);
                            }}
                            className="w-full px-3 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-[#6366F1] focus:border-transparent appearance-none cursor-pointer"
                          >
                            <option value="">전체</option>
                            <option value="very_early">22시 이전</option>
                            <option value="early">22시~00시</option>
                            <option value="normal">00시~02시</option>
                            <option value="late">02시~04시</option>
                            <option value="very_late">04시 이후</option>
                          </select>
                        </div>

                        {/* 청소 빈도 */}
                        <div>
                          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                            청소 빈도
                          </label>
                          <select
                            value={filters.cleaningFrequency || ''}
                            onChange={(e) => {
                              const newFilters = { ...filters, cleaningFrequency: e.target.value || undefined };
                              setFilters(newFilters);
                              handleApplyFilters(newFilters);
                            }}
                            className="w-full px-3 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-[#6366F1] focus:border-transparent appearance-none cursor-pointer"
                          >
                            <option value="">전체</option>
                            <option value="daily">매일</option>
                            <option value="several_times_weekly">주 2~3회</option>
                            <option value="weekly">주 1회</option>
                            <option value="monthly">월 1~2회</option>
                            <option value="rarely">거의 안함</option>
                          </select>
                        </div>

                        {/* 선호하는 연령대 */}
                        <div>
                          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                            선호하는 연령대
                          </label>
                          <select
                            value={filters.ageRange || ''}
                            onChange={(e) => {
                              const newFilters = { ...filters, ageRange: e.target.value || undefined };
                              setFilters(newFilters);
                              handleApplyFilters(newFilters);
                            }}
                            className="w-full px-3 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-[#6366F1] focus:border-transparent appearance-none cursor-pointer"
                          >
                            <option value="">전체</option>
                            <option value="20-22">20-22세</option>
                            <option value="23-25">23-25세</option>
                            <option value="26-28">26-28세</option>
                            <option value="29-30">29-30세</option>
                            <option value="31+">31세 이상</option>
                          </select>
                        </div>

                        {/* 구분선 */}
                        <div className="pt-5 border-t border-gray-200 dark:border-gray-700"></div>

                        {/* 매칭 상태 취소 버튼 */}
                        <button
                          onClick={openCancelModal}
                          className="w-full bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 py-3 rounded-lg font-medium text-sm transition-colors border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700"
                        >
                          매칭 상태 취소
                        </button>
                      </div>
                    </div>
                </div>

                  {/* 오른쪽 컨텐츠 영역 */}
                  <div className="flex-1">
                    {/* 헤더 */}
                    <div className="flex justify-between items-center mb-6">
                      <div>
                        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">추천 룸메이트</h1>
                        <p className="text-gray-600 dark:text-gray-300 mt-2">
                          {filteredUsers.length}명의 추천 룸메이트를 확인해보세요.
                        </p>
                      </div>
                      <Button onClick={handleViewResults} variant="outline">
                        확정된 룸메이트 보기
                      </Button>
                    </div>

                    {/* 그리드 레이아웃 (2x2) */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {filteredUsers.length > 0 ? (
                    filteredUsers.map(user => (
                  <UserCard
                    key={user.receiverId}
                    user={user}
                    onLikeChange={handleLikeChange}
                    onViewDetail={handleViewDetail}
                        appliedFilters={filters}
                      />
                    ))
                  ) : (
                    <div className="col-span-full text-center py-16">
                      <div className="w-20 h-20 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-4">
                        <svg className="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                        </svg>
                      </div>
                      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                        필터 조건에 맞는 룸메이트가 없습니다
                      </h3>
                      <p className="text-gray-500 dark:text-gray-400 mb-4">
                        다른 조건으로 검색해보세요
                      </p>
                      <Button onClick={handleResetFilters} variant="outline" size="sm">
                        필터 초기화
                      </Button>
                    </div>
                  )}
                    </div>
                  </div>
                </div>
              </>
        ) : (
            <PreferencePrompt onOpenModal={handleOpenPreferenceModal} />
            )}
        </div>
      </div>
      
      <MatchPreferenceModal 
        isOpen={isPreferenceModalOpen} 
        onClose={handleClosePreferenceModal}
        onSave={handlePreferenceSave}
      />
      <ConfirmModal
        isOpen={isCancelModalOpen}
        onClose={closeCancelModal}
        onConfirm={handleCancelMatching}
        title="매칭 상태 취소"
        message="정말 매칭을 취소하시겠습니까? 매칭을 취소하면 더 이상 룸메이트 추천을 받을 수 없습니다. 다시 이용하려면 선호도를 재등록해야 합니다."
        confirmText="취소하기"
        cancelText="돌아가기"
      />
      
      {/* 상세 정보 로딩 오버레이 - 모달보다 먼저 렌더링 */}
      {isDetailLoading && (
        <div className="fixed inset-0 backdrop-blur-md bg-white/50 dark:bg-gray-900/50 z-[45] flex items-center justify-center">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 shadow-xl">
            <LoadingSpinner />
            <p className="text-gray-600 dark:text-gray-400 mt-4">상세 정보를 불러오는 중...</p>
          </div>
        </div>
      )}
      
      <MatchDetailModal
        isOpen={isDetailModalOpen}
        onClose={handleCloseDetailModal}
        match={selectedUser}
        onLike={handleLikeFromModal}
        onCancelLike={handleCancelLikeFromModal}
        isLiked={selectedUser ? (selectedUser.isLiked || (selectedUser.matchType === 'LIKE' && selectedUser.matchStatus === 'PENDING')) : false}
      />
    </>
  );
}
