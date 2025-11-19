'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { User, MoreVertical, Trash2, Flag, Ban, Unlock } from 'lucide-react'
import AppHeader from '@/components/layout/AppHeader'
import ProtectedRoute from '@/components/auth/ProtectedRoute'
import { apiClient } from '@/lib/services/api'
import ReportModal from '@/components/matches/ReportModal'
import { MatchService } from '@/lib/services/matchService'
import { BlockService } from '@/lib/services/blockService'

interface ChatRoom {
  chatroomId: number
  user1Id?: number
  user2Id?: number
  partnerId?: number
  status: string
  createdAt: string
  lastMessage?: string
  lastMessageTime?: string
  partnerName?: string
  unreadCount?: number
  isNew?: boolean // NEW 배지 표시 여부
  isBlocked?: boolean // 차단 상태
}

export default function ChatListPage() {
  const router = useRouter()
  const [chats, setChats] = useState<ChatRoom[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [showMenu, setShowMenu] = useState<number | null>(null)
  const [showDeleteModal, setShowDeleteModal] = useState<number | null>(null)
  const [showReportModal, setShowReportModal] = useState<{ chatId: number; partnerName: string } | null>(null)
  const [showBlockModal, setShowBlockModal] = useState<{ chatId: number; partnerId: number; partnerName: string; isBlocked: boolean } | null>(null)
  const [isReporting, setIsReporting] = useState(false)
  const [isBlocking, setIsBlocking] = useState(false)

  // 채팅방 목록 조회 함수
  const fetchChatrooms = async () => {
    try {
      setIsLoading(true)
      // apiClient를 직접 사용 (백엔드 API 경로: /api/v1/chatrooms)
      const response = await apiClient.get('/api/v1/chatrooms')
      
      const data = response.data
      const chatrooms = data.items || []
      
      // 각 채팅방의 최신 메시지와 상대방 이름 가져오기
      const chatroomsWithMessages = await Promise.all(
        chatrooms.map(async (chat: ChatRoom) => {
          try {
            // 최신 메시지 가져오기
            const messagesResponse = await apiClient.get(
              `/api/v1/chatrooms/${chat.chatroomId}/messages`,
              { 
                params: { 
                  limit: 1,
                  order: 'desc' // 최신 메시지부터
                } 
              }
            )
            const messages = messagesResponse.data.items || []
            const lastMsg = messages[0]

            // 백엔드에서 이미 올바른 unreadCount를 계산해서 보내주므로 그대로 사용
            const unreadCount = chat.unreadCount || 0
            
            // 백엔드에서 이미 partnerName을 보내주므로 별도 조회 불필요
            const partnerName = chat.partnerName || '알 수 없는 사용자'
            
            // 차단 상태 확인
            const isBlocked = chat.isBlocked || false
            
            // NEW 배지 표시 여부 확인 (localStorage에서 방문 기록 확인)
            const visitedChatrooms = JSON.parse(localStorage.getItem('visitedChatrooms') || '[]')
            const isNew = !visitedChatrooms.includes(chat.chatroomId)
            
            return {
              ...chat,
              lastMessage: lastMsg ? lastMsg.content : '매칭되었습니다! 안녕하세요 👋',
              lastMessageTime: lastMsg ? lastMsg.createdAt : chat.createdAt,
              partnerName: partnerName,
              unreadCount: unreadCount,
              isNew: isNew,
              isBlocked: isBlocked
            }
          } catch (error) {
            // 차단 상태 확인
            const isBlocked = chat.isBlocked || false
            
            // NEW 배지 표시 여부 확인 (localStorage에서 방문 기록 확인)
            const visitedChatrooms = JSON.parse(localStorage.getItem('visitedChatrooms') || '[]')
            const isNew = !visitedChatrooms.includes(chat.chatroomId)
            
            return {
              ...chat,
              lastMessage: '매칭되었습니다! 안녕하세요 👋',
              lastMessageTime: chat.createdAt,
              partnerName: chat.partnerName || '알 수 없는 사용자',
              unreadCount: 0,
              isNew: isNew,
              isBlocked: isBlocked
            }
          }
        })
      )
      
      // 최신 메시지 시간순으로 정렬 (최신이 위로)
      const sortedChats = chatroomsWithMessages.sort((a, b) => {
        const timeA = new Date(a.lastMessageTime || a.createdAt).getTime()
        const timeB = new Date(b.lastMessageTime || b.createdAt).getTime()
        return timeB - timeA // 내림차순 (최신이 위로)
      })
      
      setChats(sortedChats)
    } catch (error) {
      setChats([])
    } finally {
      setIsLoading(false)
    }
  }

  // 채팅방 목록 조회
  useEffect(() => {
    fetchChatrooms()
  }, [])

  // 페이지가 포커스될 때 채팅 목록 새로고침 (채팅방에서 돌아왔을 때)
  useEffect(() => {
    const handleFocus = () => {
      fetchChatrooms()
    }

    window.addEventListener('focus', handleFocus)
    return () => window.removeEventListener('focus', handleFocus)
  }, [])

  const handleChatClick = async (chatId: number) => {
    // 채팅방에 들어갈 때 안 읽음 개수를 0으로 업데이트하고 NEW 배지 제거
    setChats(chats.map(chat => 
      chat.chatroomId === chatId 
        ? { ...chat, unreadCount: 0, isNew: false }
        : chat
    ))
    
    // 채팅방 진입 시 읽음 처리
    try {
      const messagesResponse = await apiClient.get(
        `/api/v1/chatrooms/${chatId}/messages`,
        { params: { limit: 1 } }
      )
      const messages = messagesResponse.data.items || []
      
      if (messages.length > 0) {
        const message = messages[0]
        const latestMessageId = message.messageId || message.id
        
        if (latestMessageId) {
          await apiClient.post(`/api/v1/chatrooms/${chatId}/read`, {
            lastReadMessageId: latestMessageId
          })
        }
      }
    } catch (error) {
      // 읽음 처리 실패는 무시
    }
    
    // 채팅방 방문 기록 저장 (NEW 배지 제거용)
    const visitedChatrooms = JSON.parse(localStorage.getItem('visitedChatrooms') || '[]')
    if (!visitedChatrooms.includes(chatId)) {
      visitedChatrooms.push(chatId)
      localStorage.setItem('visitedChatrooms', JSON.stringify(visitedChatrooms))
    }
    
    router.push(`/chat/${chatId}`)
  }

  const handleDeleteChat = async (chatId: number) => {
    try {
      // 채팅방 나가기 API 사용
      await apiClient.post(`/api/v1/chatrooms/${chatId}/leave`)
      setChats(chats.filter(chat => chat.chatroomId !== chatId))
      setShowDeleteModal(null)
      setShowMenu(null)
    } catch (error) {
      alert('채팅방 나가기에 실패했습니다. 잠시 후 다시 시도해주세요.')
    }
  }

  const handleReportUser = async (category: string, content: string) => {
    if (!showReportModal) return
    
    setIsReporting(true)
    try {
      const chat = chats.find(c => c.chatroomId === showReportModal.chatId)
      if (!chat) {
        alert('채팅방 정보를 찾을 수 없습니다.')
        return
      }
      
      // 채팅방 상세 정보에서 상대방 이메일 가져오기
      const chatroomResponse = await apiClient.get(`/api/v1/chatrooms/${chat.chatroomId}`)
      const chatroomData = chatroomResponse.data
      
      // 현재 사용자 ID
      const currentUserId = typeof window !== 'undefined' ? 
        parseInt(localStorage.getItem('userId') || '0') : 0
      
      // 상대방 ID 찾기
      const partnerId = chatroomData.user1Id === currentUserId ? chatroomData.user2Id : chatroomData.user1Id
      
      // 매칭 상태에서 해당 사용자의 이메일 찾기
      try {
        const matchStatusResponse = await apiClient.get('/api/v1/matches/status')
        const matchStatus = matchStatusResponse.data.matches || []
        
        // 현재 채팅방과 관련된 매칭 상태 찾기
        const relatedMatch = matchStatus.find((match: any) => 
          (match.senderId === currentUserId && match.receiverId === partnerId) ||
          (match.senderId === partnerId && match.receiverId === currentUserId)
        )
        
        if (!relatedMatch) {
          alert('매칭 정보를 찾을 수 없어 신고할 수 없습니다.')
          return
        }
        
        // 상대방의 이메일 찾기 (partner 필드에서)
        const reportedEmail = relatedMatch.partner?.email
        
        if (!reportedEmail) {
          alert('상대방의 이메일 정보를 찾을 수 없어 신고할 수 없습니다.')
          return
        }
        
        await MatchService.reportUser({
          reportedEmail,
          category,
          content
        })
      } catch (matchError) {
        console.error('매칭 상태 조회 실패:', matchError)
        alert('매칭 정보를 조회할 수 없어 신고할 수 없습니다.')
        return
      }
      
      alert('신고가 접수되었습니다. 검토 후 조치하겠습니다.')
      setShowReportModal(null)
    } catch (error: any) {
      console.error('신고 실패:', error)
      const errorMessage = error.response?.data?.message || error.message || '신고 접수에 실패했습니다.'
      alert(errorMessage)
    } finally {
      setIsReporting(false)
    }
  }

  const handleBlockUser = async () => {
    if (!showBlockModal) return
    
    setIsBlocking(true)
    try {
      if (showBlockModal.isBlocked) {
        // 차단 해제
        await BlockService.unblockUser(showBlockModal.partnerId)
        alert('차단이 해제되었습니다.')
      } else {
        // 차단
        await BlockService.blockUser(showBlockModal.partnerId)
        alert('사용자를 차단했습니다. 이 사용자의 메시지는 더 이상 받지 않습니다.')
      }
      
      // 채팅 목록 새로고침
      await fetchChatrooms()
      setShowBlockModal(null)
      setShowMenu(null)
    } catch (error: any) {
      console.error('차단 처리 실패:', error)
      const errorMessage = error.message || error.response?.data?.message || '차단 처리에 실패했습니다.'
      alert(errorMessage)
    } finally {
      setIsBlocking(false)
    }
  }


  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-[#F9FAFB]">
        <AppHeader />

        {/* Main Content */}
        <div className="px-4 py-6">
          {/* Title */}
          <div className="mb-2">
            <h1 className="text-2xl font-bold text-[#111827]">채팅</h1>
          </div>
          
          {/* Description */}
          <div className="mb-6">
            <p className="text-[#6B7280] text-sm">매칭된 룸메이트와 대화를 나눠보세요</p>
          </div>

          {/* Loading */}
          {isLoading ? (
            <div className="bg-white rounded-xl p-12 text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#4F46E5] mx-auto"></div>
              <p className="mt-4 text-[#6B7280]">채팅방 목록을 불러오는 중...</p>
            </div>
          ) : chats.length === 0 ? (
            <div className="bg-white rounded-xl p-12 text-center">
              <div className="w-16 h-16 bg-[#F3F4F6] rounded-full flex items-center justify-center mx-auto mb-4">
                <User className="w-8 h-8 text-[#9CA3AF]" />
              </div>
              <h3 className="text-lg font-semibold text-[#111827] mb-2">아직 매칭된 대화가 없습니다</h3>
              <p className="text-[#6B7280] mb-6">
                매칭 페이지에서 마음에 드는 프로필에 좋아요를 눌러보세요.<br />
                상대방도 좋아요를 누르면 채팅방이 생성됩니다!
              </p>
              <button 
                onClick={() => router.push('/matches')}
                className="text-[#4F46E5] font-medium hover:underline"
              >
                매칭 페이지로 가기 →
              </button>
            </div>
          ) : (
            <>
              <div className="space-y-4">
                {chats.map((chat) => (
                <div
                  key={chat.chatroomId}
                  className="bg-white rounded-xl p-6 shadow-md hover:shadow-lg transition-shadow cursor-pointer relative border border-gray-100"
                  onClick={() => {
                    // 메뉴가 열려있으면 채팅방으로 이동하지 않음
                    if (showMenu !== chat.chatroomId) {
                      handleChatClick(chat.chatroomId)
                    }
                  }}
                >
                  <div className="flex items-start gap-4">
                    {/* Content */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <h3 className="font-semibold text-[#111827]">{chat.partnerName || `사용자 ${chat.partnerId || chat.user1Id || chat.user2Id}`}</h3>
                          {chat.isNew && (
                            <span className="bg-[#EF4444] text-white text-xs px-2 py-1 rounded font-semibold">
                              NEW
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm text-[#9CA3AF]">
                            {chat.lastMessageTime 
                              ? new Date(chat.lastMessageTime).toLocaleTimeString('ko-KR', { 
                                  hour: '2-digit', 
                                  minute: '2-digit' 
                                })
                              : new Date(chat.createdAt).toLocaleDateString('ko-KR')
                            }
                          </span>
                          {chat.unreadCount && chat.unreadCount > 0 ? (
                            <span className="bg-[#4F46E5] text-white text-xs px-2 py-1 rounded-full min-w-[20px] text-center font-semibold">
                              {chat.unreadCount > 99 ? '99+' : chat.unreadCount}
                            </span>
                          ) : null}
                        </div>
                      </div>
                      <p className="text-[#6B7280] truncate">
                        {chat.lastMessage || '매칭되었습니다! 안녕하세요 👋'}
                      </p>
                    </div>

                    {/* Menu Button */}
                    <div className="relative">
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          setShowMenu(showMenu === chat.chatroomId ? null : chat.chatroomId)
                        }}
                        className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-[#F3F4F6] transition-colors"
                      >
                        <MoreVertical className="w-5 h-5 text-[#6B7280]" />
                      </button>

                      {/* Dropdown Menu */}
                      {showMenu === chat.chatroomId && (
                        <>
                          <div 
                            className="fixed inset-0 z-10" 
                            onClick={() => setShowMenu(null)}
                          />
                          <div className="absolute right-0 top-10 w-48 bg-white border border-[#E5E7EB] rounded-xl shadow-lg z-20 overflow-hidden">
                            <button
                              onClick={(e) => {
                                e.stopPropagation()
                                setShowReportModal({ chatId: chat.chatroomId, partnerName: chat.partnerName || '알 수 없는 사용자' })
                                setShowMenu(null)
                              }}
                              className="w-full px-4 py-3 flex items-center gap-3 text-left hover:bg-[#FEF3C7] transition-colors"
                            >
                              <Flag className="w-4 h-4 text-[#F59E0B]" />
                              <span className="text-sm text-[#F59E0B]">신고하기</span>
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation()
                                const currentUserId = typeof window !== 'undefined' ? 
                                  parseInt(localStorage.getItem('userId') || '0') : 0
                                const partnerId = chat.user1Id === currentUserId ? chat.user2Id : chat.user1Id || chat.partnerId || 0
                                
                                setShowBlockModal({ 
                                  chatId: chat.chatroomId, 
                                  partnerId: partnerId,
                                  partnerName: chat.partnerName || '알 수 없는 사용자',
                                  isBlocked: chat.isBlocked || false
                                })
                                setShowMenu(null)
                              }}
                              className="w-full px-4 py-3 flex items-center gap-3 text-left hover:bg-[#FEE2E2] transition-colors"
                            >
                              {chat.isBlocked ? (
                                <>
                                  <Unlock className="w-4 h-4 text-[#10B981]" />
                                  <span className="text-sm text-[#10B981]">차단 해제</span>
                                </>
                              ) : (
                                <>
                                  <Ban className="w-4 h-4 text-[#EF4444]" />
                                  <span className="text-sm text-[#EF4444]">차단하기</span>
                                </>
                              )}
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation()
                                setShowDeleteModal(chat.chatroomId)
                                setShowMenu(null)
                              }}
                              className="w-full px-4 py-3 flex items-center gap-3 text-left hover:bg-[#FEE2E2] transition-colors"
                            >
                              <Trash2 className="w-4 h-4 text-[#EF4444]" />
                              <span className="text-sm text-[#EF4444]">채팅방 나가기</span>
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))}
              </div>
            </>
          )}

          {/* Delete Chat Modal */}
          {showDeleteModal !== null && (
            <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50">
              <div className="bg-white rounded-xl p-6 w-full max-w-md mx-4">
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-12 h-12 bg-[#FEE2E2] rounded-xl flex items-center justify-center">
                    <Trash2 className="w-6 h-6 text-[#EF4444]" />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-[#111827]">채팅방 나가기</h3>
                    <p className="text-sm text-[#6B7280]">이 채팅방에서 나가시겠습니까?</p>
                  </div>
                </div>

                <div className="p-4 bg-[#FEF2F2] border border-[#FEE2E2] rounded-xl mb-4">
                  <p className="text-sm text-[#991B1B]">
                    나간 채팅방은 다시 들어갈 수 없습니다.
                  </p>
                </div>

                <div className="flex gap-3">
                  <button
                    onClick={() => setShowDeleteModal(null)}
                    className="flex-1 px-4 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    취소
                  </button>
                  <button
                    onClick={() => handleDeleteChat(showDeleteModal)}
                    className="flex-1 px-4 py-3 bg-[#EF4444] text-white rounded-lg hover:bg-[#DC2626] transition-colors"
                  >
                    삭제하기
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Report Modal */}
          {showReportModal && (
            <ReportModal
              isOpen={true}
              onClose={() => setShowReportModal(null)}
              onSubmit={handleReportUser}
              reportedUserName={showReportModal.partnerName}
              isSubmitting={isReporting}
            />
          )}

          {/* Block Modal */}
          {showBlockModal && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl p-6 w-full max-w-md mx-4">
                <div className="flex items-center gap-3 mb-4">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                    showBlockModal.isBlocked ? 'bg-[#D1FAE5]' : 'bg-[#FEE2E2]'
                  }`}>
                    {showBlockModal.isBlocked ? (
                      <Unlock className="w-6 h-6 text-[#10B981]" />
                    ) : (
                      <Ban className="w-6 h-6 text-[#EF4444]" />
                    )}
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-[#111827]">
                      {showBlockModal.isBlocked ? '차단 해제' : '사용자 차단'}
                    </h3>
                    <p className="text-sm text-[#6B7280]">
                      {showBlockModal.partnerName}님을 {showBlockModal.isBlocked ? '차단 해제' : '차단'}하시겠습니까?
                    </p>
                  </div>
                </div>

                <div className={`p-4 rounded-xl mb-4 ${
                  showBlockModal.isBlocked 
                    ? 'bg-[#ECFDF5] border border-[#D1FAE5]' 
                    : 'bg-[#FEF2F2] border border-[#FEE2E2]'
                }`}>
                  {showBlockModal.isBlocked ? (
                    <p className="text-sm text-[#065F46]">
                      차단을 해제하면 이 사용자의 메시지를 다시 받을 수 있습니다.
                    </p>
                  ) : (
                    <p className="text-sm text-[#991B1B]">
                      차단하면 이 사용자의 메시지를 더 이상 받지 않습니다.<br />
                      차단된 기간 동안의 메시지는 전달되지 않으며, 차단 해제 후에도 과거 메시지는 받을 수 없습니다.
                    </p>
                  )}
                </div>

                <div className="flex gap-3">
                  <button
                    onClick={() => setShowBlockModal(null)}
                    disabled={isBlocking}
                    className="flex-1 px-4 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
                  >
                    취소
                  </button>
                  <button
                    onClick={handleBlockUser}
                    disabled={isBlocking}
                    className={`flex-1 px-4 py-3 text-white rounded-lg transition-colors disabled:opacity-50 ${
                      showBlockModal.isBlocked
                        ? 'bg-[#10B981] hover:bg-[#059669]'
                        : 'bg-[#EF4444] hover:bg-[#DC2626]'
                    }`}
                  >
                    {isBlocking ? '처리 중...' : showBlockModal.isBlocked ? '차단 해제' : '차단하기'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </ProtectedRoute>
  )
}
