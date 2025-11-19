'use client'

import { useEffect, useRef, useState } from 'react'
import type { StompSubscription } from '@stomp/stompjs'
import type {
  WsMessagePush,
  WsSendMessageRequest,
  WsSendAckResponse,
  WsError,
} from '@/types/chat'
import { BlockService } from '@/lib/services/blockService'

function useChatroom(chatroomId: number) {
  const [messages, setMessages] = useState<WsMessagePush[]>([])
  const [isPartnerBlocked, setIsPartnerBlocked] = useState(false)
  const isPartnerBlockedRef = useRef(false)
  const subRef = useRef<StompSubscription | null>(null)
  const ackRef = useRef<StompSubscription | null>(null)
  const errRef = useRef<StompSubscription | null>(null)

  // 차단 상태 확인
  useEffect(() => {
    const checkBlockStatus = async () => {
      try {
        const blocked = await BlockService.isPartnerBlocked(chatroomId)
        setIsPartnerBlocked(blocked)
        isPartnerBlockedRef.current = blocked
      } catch (error) {
        console.error('[Block Status] Check failed:', error)
        setIsPartnerBlocked(false)
        isPartnerBlockedRef.current = false
      }
    }
    
    checkBlockStatus()
  }, [chatroomId])

  // 메시지 히스토리 로드
  useEffect(() => {
    const loadHistory = async () => {
      try {
        const { apiClient } = await import('@/lib/services/api')
        const response = await apiClient.get(`/api/v1/chatrooms/${chatroomId}/messages`, {
          params: { 
            limit: 100,
            order: 'desc'
          }
        })
        
        const historyData = response.data
        if (historyData.items && Array.isArray(historyData.items)) {
          // 차단해도 이전 채팅 내용은 볼 수 있도록 필터링하지 않음
          const historyMessages: WsMessagePush[] = historyData.items
            .map((msg: any) => ({
              messageId: msg.messageId,
              chatroomId: chatroomId,
              senderId: msg.senderId,
              type: msg.type || 'TEXT',
              content: msg.content,
              createdAt: msg.createdAt
            }))
          
          // 시간 순으로 정렬
          const sortedMessages = historyMessages.sort((a, b) => {
            return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
          })
          
          setMessages(sortedMessages)
        }
      } catch (error) {
        console.error('[Message History] Load failed:', error)
      }
    }
    
    loadHistory()
  }, [chatroomId])

  // 페이지 가시성 변경 감지 (채팅방 퇴장 감지용)
  useEffect(() => {
    const handleVisibilityChange = () => {
      // 페이지 가시성 변경 처리
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [chatroomId])

  // WebSocket 구독 설정
  useEffect(() => {
    if (typeof window === 'undefined') return
    
    let mounted = true
    
    const initWebSocket = async () => {
      try {
        const { startWs } = await import('@/lib/services/wsManager')
        const ws = await startWs()
        
        if (!mounted) return

        // 채팅방 진입을 백엔드에 알림 (이미 ChatroomService.getDetail에서 처리됨)
        // 하지만 확실히 하기 위해 WebSocket으로도 알림
        try {
          const { apiClient } = await import('@/lib/services/api')
          await apiClient.get(`/api/v1/chatrooms/${chatroomId}`)
        } catch (error) {
          // 채팅방 진입 알림 실패는 무시
        }

        // 현재 사용자 ID 가져오기
        const currentUserId = localStorage.getItem('userId')
        const currentUserIdNum = currentUserId ? parseInt(currentUserId, 10) : null

        // 채팅방 메시지 구독
        subRef.current = ws.subscribe(`/sub/chatroom.${chatroomId}`, async (msg) => {
          try {
            const body = JSON.parse(msg.body) as WsMessagePush
            
            setMessages((prev) => {
              const exists = prev.some(m => m.messageId === body.messageId)
              if (exists) return prev
              return [...prev, body]
            })
            
            // 새 메시지가 오면 즉시 읽음 처리 (상대방이 보낸 메시지만)
            if (currentUserIdNum && body.senderId !== currentUserIdNum) {
              try {
                const { apiClient } = await import('@/lib/services/api')
                await apiClient.post(`/api/v1/chatrooms/${chatroomId}/read`, {
                  lastReadMessageId: body.messageId
                })
              } catch (error) {
                // 실시간 읽음 처리 실패는 무시
              }
            }
          } catch (e) {
            console.error('[WebSocket] Message parsing error:', e)
          }
        })

        // ACK 구독
        ackRef.current = ws.subscribe('/user/queue/ack', (msg) => {
          try {
            const ack = JSON.parse(msg.body) as WsSendAckResponse
          } catch (e) {
            console.error('[WebSocket] ACK parsing error:', e)
          }
        })

        // 에러 구독
        errRef.current = ws.subscribe(`/sub/chatroom.${chatroomId}.error`, (msg) => {
          try {
            const err = JSON.parse(msg.body)
            
            if (!err || Object.keys(err).length === 0) return
            
            const errorMessage = err.error || err.message || '알 수 없는 오류'
            
            if (errorMessage.includes('닫힌 채팅방') || 
                errorMessage.includes('차단된 채팅방') ||
                errorMessage.includes('CLOSED') ||
                errorMessage.includes('종료된 채팅방')) {
              alert('메시지 전송 실패\n\n상대방이 채팅방에서 나갔습니다.')
            } else {
              alert(`메시지 전송 실패\n\n${errorMessage}`)
            }
          } catch (e) {
            console.error('[WebSocket] Error parsing error:', e)
            alert('메시지 전송에 실패했습니다.')
          }
        })
      } catch (e) {
        console.error('[WebSocket] Connection error:', e)
        if (e instanceof Error && e.message.includes('Access token is required')) {
          return
        }
        setTimeout(() => {
          if (mounted) initWebSocket()
        }, 5000)
      }
    }
    
    initWebSocket()
    
    return () => {
      mounted = false
      subRef.current?.unsubscribe()
      ackRef.current?.unsubscribe()
      errRef.current?.unsubscribe()
      
      // 채팅방 퇴장을 백엔드에 알림
      const notifyLeave = async () => {
        try {
          const { apiClient } = await import('@/lib/services/api')
          
          // 1. 채팅방 퇴장 알림
          try {
            await apiClient.post(`/api/v1/chatrooms/${chatroomId}/leave-notification`)
          } catch (error) {
            // 채팅방 퇴장 알림 API 없음은 정상
          }
          
          // 2. 해당 채팅방의 최신 메시지를 읽음 처리 (동기적으로 실행)
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
            // 퇴장 시 읽음 처리 실패는 무시
          }
        } catch (error) {
          // 채팅방 퇴장 처리 중 오류는 무시
        }
      }
      
      notifyLeave()
    }
  }, [chatroomId])

  const send = async (content: string) => {
    if (!content.trim()) return
    if (typeof window === 'undefined') return
    
    try {
      const { getWs, startWs } = await import('@/lib/services/wsManager')
      
      let ws
      try {
        ws = getWs()
      } catch {
        ws = await startWs()
      }
      
      const clientMessageId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
      const payload: WsSendMessageRequest = {
        chatroomId,
        content,
        type: 'TEXT',
        clientMessageId
      }
      
      ws.publish('/pub/chat.send', payload)
    } catch (e) {
      console.error('[Send error]', e)
      alert('메시지 전송에 실패했습니다.')
    }
  }

  const reconnect = async () => {
    if (typeof window === 'undefined') return
    
    try {
      const { restartWs } = await import('@/lib/services/wsManager')
      await restartWs()
    } catch (e) {
      console.error('[Reconnect error]', e)
    }
  }

  return { messages, send, reconnect, isPartnerBlocked }
}

export default useChatroom
