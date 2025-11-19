'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import useChatroom from '@/hooks/useChatroom'
import { ArrowLeft, Send, CheckCircle, XCircle } from 'lucide-react'
import AppHeader from '@/components/layout/AppHeader'
import { apiClient } from '@/lib/services/api'
import { MatchService } from '@/lib/services/matchService'

interface ChatRoomViewProps {
  chatroomId: number
}

interface MatchInfo {
  matchId: number
  matchType: string
  matchStatus: string
  partnerId: number
  partnerName: string
  myResponse?: string          // 내 응답 상태
  partnerResponse?: string     // 상대방 응답 상태
  waitingForPartner?: boolean  // 상대방 응답 대기 중
}

export default function ChatRoomView({ chatroomId }: ChatRoomViewProps) {
  const router = useRouter()
  const { messages, send, reconnect, isPartnerBlocked } = useChatroom(chatroomId)
  const [text, setText] = useState('')
  const [partnerName, setPartnerName] = useState('채팅 상대')
  const [partnerInfo, setPartnerInfo] = useState('')
  const [isPartnerDeleted, setIsPartnerDeleted] = useState(false)
  const [isPartnerLeft, setIsPartnerLeft] = useState(false)
  const [isBlockedByPartner, setIsBlockedByPartner] = useState(false)  // 상대방이 나를 차단했는지
  const [matchInfo, setMatchInfo] = useState<MatchInfo | null>(null)
  const [isProcessing, setIsProcessing] = useState(false)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)
  
  // 스크롤 관련 state
  const [isInitialLoad, setIsInitialLoad] = useState(true)
  const [showNewMessageButton, setShowNewMessageButton] = useState(false)
  const [lastMessageCount, setLastMessageCount] = useState(0)
  
  // Refs
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const messagesContainerRef = useRef<HTMLDivElement>(null)

  // 채팅방 정보 조회 및 읽음 처리
  useEffect(() => {
    const fetchChatroomInfo = async () => {
      try {
        const response = await apiClient.get(`/api/v1/chatrooms/${chatroomId}`)
        const chatroomData = response.data
        
        // 백엔드에서 이미 partnerName, partnerUniversity, isPartnerDeleted를 보내줌
        setPartnerName(chatroomData.partnerName || '채팅 상대')
        setPartnerInfo(chatroomData.partnerUniversity || '')
        setIsPartnerDeleted(chatroomData.isPartnerDeleted || false)
        
        // 상대방이 나간 상태인지 확인
        const isPartnerLeft = chatroomData.user1Status === 'CLOSED' || chatroomData.user2Status === 'CLOSED'
        setIsPartnerLeft(isPartnerLeft)
        
        // 상대방이 나를 차단했는지 확인
        setIsBlockedByPartner(chatroomData.isBlockedByPartner || false)

        // 매칭 정보 조회
        const currentUserId = typeof window !== 'undefined' ? 
          parseInt(localStorage.getItem('userId') || '0') : 0
        const partnerId = chatroomData.user1Id === currentUserId ? chatroomData.user2Id : chatroomData.user1Id

        try {
          const matchStatusResponse = await MatchService.getMatchStatus()
          const matchData = matchStatusResponse.data || matchStatusResponse
          
          // 백엔드 DTO 구조: { matches: [...], summary: {...} }
          const items = matchData.matches || matchData.items || []

          // 현재 채팅 상대와의 매칭 정보 찾기
          const currentMatch = items.find((item: any) => 
            (item.senderId === currentUserId && item.receiverId === partnerId) ||
            (item.senderId === partnerId && item.receiverId === currentUserId)
          )

          // REQUEST 타입인 매칭 정보 처리
          if (currentMatch && currentMatch.matchType === 'REQUEST') {
            const myResponse = currentMatch.myResponse || 'PENDING'
            const partnerResponse = currentMatch.partnerResponse || 'PENDING'
            
            // 내가 아직 응답하지 않았고, 최종 상태가 거절이 아닌 경우에만 버튼 표시
            if (myResponse === 'PENDING' && currentMatch.matchStatus !== 'REJECTED') {
              setMatchInfo({
                matchId: currentMatch.id || currentMatch.matchId,
                matchType: currentMatch.matchType,
                matchStatus: currentMatch.matchStatus,
                partnerId: partnerId,
                partnerName: chatroomData.partnerName || '채팅 상대',
                myResponse: myResponse,
                partnerResponse: partnerResponse,
                waitingForPartner: currentMatch.waitingForPartner || false
              })
            }
          }
        } catch (error) {
          console.error('매칭 정보 조회 실패:', error)
        }

        // 채팅방에 들어갔을 때 읽음 처리
        try {
          const messagesResponse = await apiClient.get(
            `/api/v1/chatrooms/${chatroomId}/messages`,
            { params: { limit: 1 } }
          )
          const messages = messagesResponse.data.items || []
          
          if (messages.length > 0) {
            const message = messages[0]
            const latestMessageId = message.messageId || message.id
            
            if (latestMessageId) {
              await apiClient.post(`/api/v1/chatrooms/${chatroomId}/read`, {
                lastReadMessageId: latestMessageId
              })
            }
          }
        } catch (error) {
          // 읽음 처리 실패는 무시 (백엔드 트랜잭션 충돌 가능)
        }
      } catch (error) {
        console.error('채팅방 정보 조회 실패:', error)
      }
    }
    
    fetchChatroomInfo()
  }, [chatroomId])

  // 스크롤 위치 감지
  const handleScroll = () => {
    if (!messagesContainerRef.current) return
    
    const container = messagesContainerRef.current
    const isAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 100
    
    if (isAtBottom) {
      setShowNewMessageButton(false)
    }
  }
  
  // 메시지 자동 스크롤
  useEffect(() => {
    if (messages.length > 0) {
      if (isInitialLoad) {
        // 초기 로드 시에는 즉시 스크롤
        messagesEndRef.current?.scrollIntoView({ behavior: 'auto' })
        setIsInitialLoad(false)
        setLastMessageCount(messages.length)
      } else {
        // 새 메시지가 올 때
        if (messages.length > lastMessageCount) {
          const container = messagesContainerRef.current
          if (container) {
            const isAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 100
            
            if (isAtBottom) {
              // 사용자가 맨 아래에 있으면 자동 스크롤
              messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
            } else {
              // 사용자가 위에 있으면 새 메시지 버튼 표시
              setShowNewMessageButton(true)
            }
          }
          setLastMessageCount(messages.length)
        }
      }
    }
  }, [messages, isInitialLoad, lastMessageCount])
  
  // 새 메시지 버튼 클릭 시 맨 아래로 스크롤
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    setShowNewMessageButton(false)
  }

  const sendMessage = (content: string) => {
    if (!content.trim()) return
    // 차단된 사용자에게는 메시지를 보낼 수 없음
    if (isPartnerBlocked) {
      alert('차단된 사용자에게는 메시지를 보낼 수 없습니다.')
      return
    }
    // 상대방이 나를 차단한 경우 메시지를 보낼 수 없음
    if (isBlockedByPartner) {
      alert('메시지를 보낼 수 없습니다.')
      return
    }
    send(content)
    setText('')
  }

  const handleConfirmMatch = async () => {
    if (!matchInfo) return
    
    if (!confirm(`${matchInfo.partnerName}님과 룸메이트로 최종 확정하시겠습니까?\n확정 후에는 취소할 수 없습니다.`)) {
      return
    }
    
    setIsProcessing(true)
    try {
      const response = await MatchService.confirmMatch(matchInfo.matchId)
      const matchData = response.data || response
      
      // 최종 매칭 상태 확인
      const finalStatus = matchData.matchStatus || matchData.match_status
      const isFullyMatched = finalStatus === 'ACCEPTED'
      
      if (isFullyMatched) {
        // 🎉 양쪽 모두 확정 완료 - 매칭 성사!
        send(`🎉 [매칭 성사!] 축하합니다! ${matchInfo.partnerName}님과 룸메이트 매칭이 최종 확정되었습니다!`)
        
        setToast({ 
          message: '🎉 축하합니다!\n룸메이트 매칭이 최종 성사되었습니다!', 
          type: 'success' 
        })
      } else {
        // ⏰ 한쪽만 확정 - 상대방 응답 대기
        send(`✅ [매칭 확정] 룸메이트 확정을 수락했습니다! 상대방의 응답을 기다리고 있습니다.`)
        
        setToast({ 
          message: '✅ 확정 의사를 전달했습니다!\n상대방도 확정하면 매칭이 최종 성사됩니다.', 
          type: 'success' 
        })
      }
      
      setMatchInfo(null) // 버튼 숨기기
      setTimeout(() => setToast(null), 4000)
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || '매칭 확정에 실패했습니다.'
      
      // 중복 응답 에러 처리
      if (errorMessage.includes('이미 응답')) {
        setToast({ 
          message: '⚠️ 이미 확정 의사를 전달했습니다.', 
          type: 'error' 
        })
        setMatchInfo(null) // 버튼 숨기기
      } else {
        setToast({ message: errorMessage, type: 'error' })
      }
      
      setTimeout(() => setToast(null), 3000)
    } finally {
      setIsProcessing(false)
    }
  }

  const handleRejectMatch = async () => {
    if (!matchInfo) return
    
    if (!confirm(`${matchInfo.partnerName}님과의 룸메이트 매칭을 거절하시겠습니까?\n거절 시 다시 되돌릴 수 없습니다.`)) {
      return
    }
    
    setIsProcessing(true)
    try {
      await MatchService.rejectMatch(matchInfo.matchId)
      
      // 📢 상대방에게 자동 메시지 전송
      send(`❌ [매칭 거절] 죄송하지만 이번 매칭은 거절했습니다. 더 나은 기회에 인연이 닿기를 바랍니다.`)
      
      // 거절 완료 안내
      setToast({ 
        message: '❌ 매칭을 거절했습니다.\n더 나은 룸메이트를 찾아보세요!', 
        type: 'success' 
      })
      setMatchInfo(null) // 버튼 숨기기
      
      setTimeout(() => setToast(null), 3000)
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || '매칭 거절에 실패했습니다.'
      
      // 중복 응답 에러 처리
      if (errorMessage.includes('이미 응답')) {
        setToast({ 
          message: '⚠️ 이미 거절 의사를 전달했습니다.', 
          type: 'error' 
        })
        setMatchInfo(null) // 버튼 숨기기
      } else {
        setToast({ message: errorMessage, type: 'error' })
      }
      
      setTimeout(() => setToast(null), 3000)
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <div className="h-screen bg-[#F9FAFB] flex flex-col relative">
      <AppHeader />
      
      {/* Toast Message */}
      {toast && (
        <div className="fixed top-20 left-1/2 transform -translate-x-1/2 z-50 animate-fade-in">
          <div className={`px-6 py-3 rounded-lg shadow-lg ${
            toast.type === 'success' ? 'bg-[#10B981] text-white' : 'bg-[#EF4444] text-white'
          }`}>
            <p className="font-medium">{toast.message}</p>
          </div>
        </div>
      )}

      {/* Chat Info Bar */}
      <div className="bg-white border-b border-[#E5E7EB] flex-shrink-0">
        <div className="px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => router.back()}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <ArrowLeft className="w-5 h-5 text-gray-600" />
              </button>
              <div>
                <h2 className={`font-semibold ${isPartnerDeleted ? 'text-red-500' : 'text-[#111827]'}`}>
                  {partnerName}
                </h2>
                {isPartnerDeleted ? (
                  <p className="text-sm text-red-400">이 사용자는 탈퇴했습니다</p>
                ) : isPartnerLeft ? (
                  <p className="text-sm text-red-500">상대방이 채팅방에서 나갔습니다</p>
                ) : isPartnerBlocked ? (
                  <p className="text-sm text-orange-500">이 사용자를 차단했습니다</p>
                ) : (
                  partnerInfo && <p className="text-sm text-[#6B7280]">{partnerInfo}</p>
                )}
              </div>
            </div>

            {/* 매칭 확정/거절 버튼 */}
            {matchInfo && !isPartnerDeleted && !isPartnerLeft && (
              <div className="flex flex-col items-end gap-2">
                {/* 상대방 응답 상태 표시 */}
                {matchInfo.partnerResponse === 'ACCEPTED' && (
                  <div className="bg-blue-50 text-blue-700 px-4 py-2 rounded-lg text-sm font-medium border border-blue-200 shadow-sm">
                    ⏰ 상대방이 확정을 기다리고 있습니다
                  </div>
                )}
                {matchInfo.partnerResponse === 'REJECTED' && (
                  <div className="bg-red-50 text-red-700 px-4 py-2 rounded-lg text-sm font-medium border border-red-200 shadow-sm">
                    ❌ 상대방이 거절했습니다
                  </div>
                )}
                
                {/* 확정/거절 버튼 */}
                <div className="flex items-center gap-3">
                  <button
                    onClick={handleRejectMatch}
                    disabled={isProcessing}
                    className="group relative px-5 py-2.5 bg-white border border-gray-300 rounded-xl hover:border-[#EF4444] hover:bg-[#FEF2F2] disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm hover:shadow-md"
                  >
                    <div className="flex items-center gap-2">
                      <XCircle className="w-4 h-4 text-gray-600 group-hover:text-[#EF4444] transition-colors" />
                      <span className="font-semibold text-sm text-gray-700 group-hover:text-[#EF4444] transition-colors">
                        거절
                      </span>
                    </div>
                  </button>
                  <button
                    onClick={handleConfirmMatch}
                    disabled={isProcessing}
                    className="group relative px-5 py-2.5 bg-gradient-to-r from-[#4F46E5] to-[#6366F1] text-white rounded-xl hover:from-[#4338CA] hover:to-[#4F46E5] disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-md hover:shadow-lg"
                  >
                    <div className="flex items-center gap-2">
                      <CheckCircle className="w-4 h-4" />
                      <span className="font-semibold text-sm">
                        룸메이트 확정
                      </span>
                    </div>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Messages Container */}
      <div 
        ref={messagesContainerRef}
        className="flex-1 overflow-y-auto px-4 py-6 relative"
        onScroll={handleScroll}
      >
        <div className="space-y-4">
          {messages.length === 0 ? (
            <div className="text-center text-gray-500 py-8">
              아직 메시지가 없습니다. 첫 메시지를 보내보세요!
            </div>
          ) : (
            messages.map((m) => {
              // 현재 사용자 ID를 localStorage에서 가져오기
              const currentUserId = typeof window !== 'undefined' ? 
                parseInt(localStorage.getItem('userId') || '0') : 0
              const isMe = m.senderId === currentUserId
              const messageTime = new Date(m.createdAt).toLocaleTimeString('ko-KR', { 
                hour: '2-digit', 
                minute: '2-digit' 
              })
              
              return (
                <div
                  key={m.messageId}
                  className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-md px-4 py-3 rounded-2xl ${
                      isMe
                        ? 'bg-[#4F46E5] text-white'
                        : 'bg-white border border-[#E5E7EB] text-[#111827]'
                    }`}
                  >
                    <p className="mb-1">{m.content}</p>
                    <p
                      className={`text-xs ${
                        isMe ? 'text-[#C7D2FE]' : 'text-[#9CA3AF]'
                      }`}
                    >
                      {messageTime}
                    </p>
                  </div>
                </div>
              )
            })
          )}
          {/* 스크롤을 위한 빈 div */}
          <div ref={messagesEndRef} />
        </div>
      </div>
      
      {/* 새 메시지 버튼 - 메시지 컨테이너 밖에 고정 */}
      {showNewMessageButton && (
        <div className="absolute bottom-28 left-1/2 transform -translate-x-1/2 z-20">
          <button
            onClick={scrollToBottom}
            className="bg-[#4F46E5] text-white px-4 py-2 rounded-full shadow-lg hover:bg-[#4338CA] transition-colors flex items-center gap-2"
          >
            <span className="text-sm font-medium">새 메시지</span>
            <div className="w-2 h-2 bg-white rounded-full animate-pulse"></div>
          </button>
        </div>
      )}

      {/* Message Input */}
      <div className="bg-white border-t border-[#E5E7EB] flex-shrink-0">
        <div className="px-4 py-4">
          {isPartnerDeleted ? (
            <div className="text-center py-4 text-gray-500 bg-gray-50 rounded-xl">
              <p className="text-sm">탈퇴한 사용자와는 메시지를 주고받을 수 없습니다</p>
            </div>
          ) : isPartnerLeft ? (
            <div className="text-center py-4 text-gray-500 bg-gray-50 rounded-xl">
              <p className="text-sm">상대방이 채팅방에서 나갔습니다</p>
            </div>
          ) : isPartnerBlocked ? (
            <div className="text-center py-4 text-orange-500 bg-orange-50 rounded-xl border border-orange-200">
              <p className="text-sm font-medium">차단된 사용자입니다. 메시지를 보낼 수 없습니다.</p>
            </div>
          ) : isBlockedByPartner ? (
            <div className="text-center py-4 text-gray-500 bg-gray-50 rounded-xl">
              <p className="text-sm font-medium">메시지를 보낼 수 없습니다.</p>
            </div>
          ) : (
            <div className="flex gap-3">
              <input
                placeholder="메시지를 입력하세요..."
                value={text}
                onChange={(e) => setText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault()
                    sendMessage(text)
                  }
                }}
                className="flex-1 px-4 py-3 border border-[#E5E7EB] rounded-xl focus:outline-none focus:ring-2 focus:ring-[#4F46E5] focus:border-transparent"
              />
              <button
                onClick={() => sendMessage(text)}
                disabled={!text.trim()}
                className="w-12 h-12 bg-[#4F46E5] text-white rounded-xl flex items-center justify-center hover:bg-[#4338CA] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <Send className="w-5 h-5" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
