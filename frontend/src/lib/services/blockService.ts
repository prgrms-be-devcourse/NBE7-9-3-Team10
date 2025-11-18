import { apiClient } from './api'
import { ApiResponse } from '@/types/api'

export interface BlockedUser {
  userId: number
  userName: string
  blockedAt: string
}

export interface BlockStatus {
  isBlocked: boolean
  blockedAt?: string
}

/**
 * 사용자 차단 서비스
 */
export class BlockService {
  /**
   * 사용자를 차단합니다
   * @param userId - 차단할 사용자 ID
   */
  static async blockUser(userId: number): Promise<void> {
    try {
      await apiClient.post(`/api/v1/users/${userId}/block`)
    } catch (error: any) {
      console.error('[BlockService] blockUser error:', error)
      console.error('[BlockService] error.response:', error.response)
      
      // HTTP 상태 코드에 따른 에러 메시지
      if (error.response?.status === 404) {
        throw new Error('차단 API가 아직 구현되지 않았습니다. 백엔드 개발자에게 문의하세요.')
      } else if (error.response?.status === 401) {
        throw new Error('로그인이 필요합니다.')
      } else if (error.response?.status === 403) {
        throw new Error('차단 권한이 없습니다.')
      } else if (error.response?.status === 400) {
        const errorMessage = error.response?.data?.message || '잘못된 요청입니다.'
        throw new Error(errorMessage)
      }
      
      const errorMessage = error.response?.data?.message || error.message || '차단에 실패했습니다.'
      throw new Error(errorMessage)
    }
  }

  /**
   * 사용자 차단을 해제합니다
   * @param userId - 차단 해제할 사용자 ID
   */
  static async unblockUser(userId: number): Promise<void> {
    try {
      await apiClient.delete(`/api/v1/users/${userId}/block`)
    } catch (error: any) {
      console.error('[BlockService] unblockUser error:', error)
      console.error('[BlockService] error.response:', error.response)
      
      // HTTP 상태 코드에 따른 에러 메시지
      if (error.response?.status === 404) {
        throw new Error('차단 해제 API가 아직 구현되지 않았습니다. 백엔드 개발자에게 문의하세요.')
      } else if (error.response?.status === 401) {
        throw new Error('로그인이 필요합니다.')
      } else if (error.response?.status === 403) {
        throw new Error('차단 해제 권한이 없습니다.')
      } else if (error.response?.status === 400) {
        const errorMessage = error.response?.data?.message || '잘못된 요청입니다.'
        throw new Error(errorMessage)
      }
      
      const errorMessage = error.response?.data?.message || error.message || '차단 해제에 실패했습니다.'
      throw new Error(errorMessage)
    }
  }

  /**
   * 차단된 사용자 목록을 조회합니다
   */
  static async getBlockedUsers(): Promise<BlockedUser[]> {
    try {
      const response = await apiClient.get<ApiResponse<BlockedUser[]>>('/api/v1/users/me/blocked')
      return response.data?.data || response.data || []
    } catch (error: any) {
      console.error('차단 목록 조회 실패:', error)
      return []
    }
  }

  /**
   * 특정 사용자가 차단되었는지 확인합니다
   * @param userId - 확인할 사용자 ID
   */
  static async isUserBlocked(userId: number): Promise<boolean> {
    try {
      const blockedUsers = await this.getBlockedUsers()
      return blockedUsers.some(user => user.userId === userId)
    } catch (error) {
      console.error('차단 상태 확인 실패:', error)
      return false
    }
  }

  /**
   * 채팅방에서 상대방이 차단되었는지 확인합니다
   * @param chatroomId - 채팅방 ID
   */
  static async isPartnerBlocked(chatroomId: number): Promise<boolean> {
    try {
      const response = await apiClient.get(`/api/v1/chatrooms/${chatroomId}`)
      const chatroomData = response.data
      return chatroomData.isBlocked || false
    } catch (error) {
      console.error('차단 상태 확인 실패:', error)
      return false
    }
  }
}

