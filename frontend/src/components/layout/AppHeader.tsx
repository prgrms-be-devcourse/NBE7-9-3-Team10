'use client'

import { useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { Home, Users, MessageCircle, User, Star, Bell, LogOut } from 'lucide-react'
import { useNotifications } from '@/hooks/useNotifications'
import NotificationModal from '@/components/notification/NotificationModal'
import MatchDetailModal from '@/components/matches/MatchDetailModal'
import { useToast } from '@/components/ui/Toast'
import { stopWs } from '@/lib/services/wsManager'
import { useAuth } from '@/contexts/AuthContext'
import { NotificationService } from '@/lib/services/notificationService'
import { MatchService } from '@/lib/services/matchService'
import { getErrorMessage } from '@/lib/utils/helpers'
import type { MatchRecommendationDetailResponse } from '@/types/match'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import Link from 'next/link'
import PendingReviewList from '@/components/review/PendingReviewList'
import WrittenReviewList from '@/components/review/WrittenReviewList'
import { Dialog, Transition } from '@headlessui/react'
import { Fragment } from 'react'

export default function AppHeader() {
  const router = useRouter()
  const pathname = usePathname()
  const [isNotificationOpen, setIsNotificationOpen] = useState(false)
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false)
  const [reviewTab, setReviewTab] = useState<'pending' | 'written'>('pending')
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false)
  const [selectedUser, setSelectedUser] = useState<MatchRecommendationDetailResponse | null>(null)
  const [isDetailLoading, setIsDetailLoading] = useState(false)
  const { notifications, unreadCount, markAsRead, deleteNotification, deleteAllNotifications, checkWebSocketStatus } = useNotifications()
  const { success, error: showError } = useToast()
  const { isAuthenticated, isLoading, logout } = useAuth()

  const navigationItems = [
    { key: '/', label: '홈', icon: Home },
    { key: '/matches', label: '매칭', icon: Users },
    { key: '/chat', label: '채팅', icon: MessageCircle },
    { key: '/profile', label: '프로필', icon: User },
  ]

  const handleLogout = async () => {
    try {
      await stopWs()
      await logout()
      success('로그아웃되었습니다.', '로그아웃 완료')
      window.location.href = '/'
    } catch (error) {
      success('로그아웃되었습니다.', '로그아웃 완료')
      window.location.href = '/'
    }
  }

  const handleViewProfile = async (senderId: number) => {
    setIsDetailLoading(true)
    setIsDetailModalOpen(false)
    setIsNotificationOpen(false)
    
    try {
      const detail = await MatchService.getMatchDetail(senderId)
      setSelectedUser(detail)
      setIsDetailLoading(false)
      setIsDetailModalOpen(true)
    } catch (err) {
      console.error('상세 정보 조회 실패:', err)
      setIsDetailLoading(false)
      showError(
        getErrorMessage(err) || "프로필 정보를 불러오는데 실패했습니다.",
        "프로필 조회 실패"
      )
      router.push(`/profile/${senderId}`)
    }
  }

  const handleViewChat = (chatroomId: number) => {
    router.push(`/chat/${chatroomId}`)
    setIsNotificationOpen(false)
  }

  const handleCloseDetailModal = () => {
    setIsDetailModalOpen(false)
    setSelectedUser(null)
  }

  const handleLikeFromModal = async (receiverId: number) => {
    await MatchService.sendLike(receiverId)
  }

  const handleCancelLikeFromModal = async (receiverId: number) => {
    await MatchService.cancelLike(receiverId)
  }

  const handleCheckWebSocketStatus = async () => {
    await checkWebSocketStatus()
  }

  const handleSendTestNotification = async () => {
    try {
      await NotificationService.getNotifications(0, 5)
      success('알림 목록을 새로고침했습니다. 다른 계정으로 알림을 생성해보세요.', '테스트 안내')
    } catch (error) {
      // 테스트 알림 전송 실패는 무시
    }
  }

  if (isLoading) {
    return (
      <header className="bg-white border-b border-[#E5E7EB] sticky top-0 z-20">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-center">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-[#4F46E5]"></div>
        </div>
      </header>
    )
  }

  if (!isAuthenticated) {
    return (
      <header className="bg-white border-b border-[#E5E7EB] sticky top-0 z-20">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
            <div className="w-8 h-8 bg-[#4F46E5] rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">U</span>
            </div>
            <span className="text-xl font-bold text-[#4F46E5]">UniMate</span>
          </Link>

          <div className="flex items-center gap-3">
            <Link href="/login">
              <button className="px-4 py-2 text-gray-700 hover:bg-gray-50 rounded-lg transition-colors">
                로그인
              </button>
            </Link>
            <Link href="/register">
              <button className="px-4 py-2 bg-[#4F46E5] text-white hover:bg-[#4338CA] rounded-lg transition-colors">
                회원가입
              </button>
            </Link>
          </div>
        </div>
      </header>
    )
  }

  return (
    <header className="bg-white border-b border-[#E5E7EB] sticky top-0 z-20">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
        <button 
          onClick={() => router.push('/')}
          className="flex items-center gap-2 hover:opacity-80 transition-opacity"
        >
          <div className="w-8 h-8 bg-[#4F46E5] rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">U</span>
          </div>
          <span className="text-xl font-bold text-[#4F46E5]">UniMate</span>
        </button>

        <nav className="flex items-center gap-6">
          {navigationItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.key
            
            return (
              <button
                key={item.key}
                onClick={() => router.push(item.key)}
                className={`flex items-center gap-1 px-3 py-1 rounded-lg transition-colors ${
                  isActive
                    ? 'text-[#4F46E5] bg-[#EEF2FF] font-semibold'
                    : 'text-[#6B7280] hover:bg-gray-50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="text-sm">{item.label}</span>
              </button>
            )
          })}
        </nav>

        <div className="flex items-center gap-4">
          {/* 별 아이콘 버튼 추가 */}
          <button
            onClick={() => setIsReviewModalOpen(true)}
            className="p-1 hover:bg-gray-100 rounded-lg transition-colors"
            title="후기"
          >
            <Star className="w-5 h-5 text-[#F59E0B]" />
          </button>
          
          <div className="relative flex items-center">
            <button
              onClick={() => setIsNotificationOpen(true)}
              className="p-1 hover:bg-gray-100 rounded-lg transition-colors"
              title="알림"
            >
              <Bell className="w-5 h-5 text-[#6B7280]" />
            </button>
            {unreadCount > 0 && (
              <div className="absolute -top-1 -right-1 w-4 h-4 bg-[#4F46E5] text-white text-xs rounded-full flex items-center justify-center">
                {unreadCount > 99 ? '99+' : unreadCount}
              </div>
            )}
          </div>
          
          <button 
            onClick={handleLogout}
            className="flex items-center hover:bg-gray-100 rounded-lg transition-colors"
            title="로그아웃"
          >
            <LogOut className="w-5 h-5 text-[#6B7280]" />
          </button>
        </div>
      </div>

      {/* 후기 모달 추가 */}
      <Transition appear show={isReviewModalOpen} as={Fragment}>
        <Dialog as="div" className="relative z-50" onClose={() => setIsReviewModalOpen(false)}>
          <Transition.Child
            as={Fragment}
            enter="ease-out duration-300"
            enterFrom="opacity-0"
            enterTo="opacity-100"
            leave="ease-in duration-200"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
          >
            {/* 블러 처리 - 배경 투명도 낮춤 */}
            <div className="fixed inset-0 bg-black/20 backdrop-blur-sm" />
          </Transition.Child>

          <div className="fixed inset-0 overflow-hidden">
            <div className="flex min-h-full items-start justify-end">
              <Transition.Child
                as={Fragment}
                enter="ease-in-out duration-500"
                enterFrom="opacity-0 translate-x-full"
                enterTo="opacity-100 translate-x-0"
                leave="ease-in-out duration-300"
                leaveFrom="opacity-100 translate-x-0"
                leaveTo="opacity-0 translate-x-full"
              >
                <Dialog.Panel className="h-full w-full max-w-md bg-white shadow-2xl overflow-y-auto">
                  <div className="sticky top-0 bg-white border-b border-gray-200 p-6 z-10">
                    <div className="flex items-center justify-between mb-2">
                      <Dialog.Title className="text-2xl font-bold text-gray-900">
                        리뷰
                      </Dialog.Title>
                      <button
                        onClick={() => setIsReviewModalOpen(false)}
                        className="text-gray-400 hover:text-gray-600 transition-colors p-1"
                      >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                    
                    {/* 탭 추가 */}
                    <div className="flex gap-2 mt-4 border-b border-gray-200">
                      <button
                        onClick={() => setReviewTab('pending')}
                        className={`px-4 py-2 text-sm font-medium transition-colors ${
                          reviewTab === 'pending'
                            ? 'text-[#4F46E5] border-b-2 border-[#4F46E5]'
                            : 'text-gray-500 hover:text-gray-700'
                        }`}
                      >
                        대기 중인 리뷰
                      </button>
                      <button
                        onClick={() => setReviewTab('written')}
                        className={`px-4 py-2 text-sm font-medium transition-colors ${
                          reviewTab === 'written'
                            ? 'text-[#4F46E5] border-b-2 border-[#4F46E5]'
                            : 'text-gray-500 hover:text-gray-700'
                        }`}
                      >
                        작성된 리뷰
                      </button>
                    </div>
                  </div>
                  
                  <div className="p-6">
                    {reviewTab === 'pending' ? (
                      <PendingReviewList />
                    ) : (
                      <WrittenReviewList />
                    )}
                  </div>
                </Dialog.Panel>
              </Transition.Child>
            </div>
          </div>
        </Dialog>
      </Transition>

      <NotificationModal
        isOpen={isNotificationOpen}
        onClose={() => setIsNotificationOpen(false)}
        notifications={notifications}
        onMarkAsRead={markAsRead}
        onDeleteNotification={deleteNotification}
        onDeleteAllNotifications={deleteAllNotifications}
        onViewProfile={handleViewProfile}
        onViewChat={handleViewChat}
      />

      {/* 상세 정보 로딩 오버레이 */}
      {isDetailLoading && (
        <div className="fixed inset-0 backdrop-blur-sm bg-black/30 z-[45] flex items-center justify-center">
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
    </header>
  )
}