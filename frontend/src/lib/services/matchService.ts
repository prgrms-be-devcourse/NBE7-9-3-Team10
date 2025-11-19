import { api, API_ENDPOINTS } from '@/lib/services/api';
import { ApiResponse } from '@/types/api';
import { MatchPreference } from '@/types/profile'; 

// 매칭 관련 API 서비스
export class MatchService {
  /**
   * 룸메이트 추천 목록을 조회합니다.
   */
  static async getRecommendations(filters?: any): Promise<ApiResponse<any>> {
    // TODO: 백엔드 DTO에 맞춰 타입 정의 필요
    return api.get<ApiResponse<any>>(API_ENDPOINTS.MATCH_RECOMMENDATIONS, { params: filters });
  }

  /**
   * 사용자의 매칭 상태를 조회합니다.
   */
  static async getMatchStatus(): Promise<ApiResponse<any>> {
    return api.get<ApiResponse<any>>(API_ENDPOINTS.MATCH_STATUS);
  }

  /**
   * 사용자의 매칭 선호도를 업데이트합니다.
   */
  static async updatePreference(preferenceData: MatchPreference): Promise<ApiResponse<any>> {
    return api.put<ApiResponse<any>>(API_ENDPOINTS.USER_PREFERENCES, preferenceData);
  }

  /**
   * 사용자의 매칭 상태를 비활성화(취소)합니다.
   */
  static async cancelMatching(): Promise<ApiResponse<void>> {
    return api.delete<ApiResponse<void>>(API_ENDPOINTS.USER_DELETE_MATCHING_STATUS);
  }

  /**
   * 다른 사용자에게 '좋아요'를 보냅니다.
   */
  static async sendLike(receiverId: number): Promise<ApiResponse<any>> {
    return api.post<ApiResponse<any>>(API_ENDPOINTS.MATCH_LIKES, { receiverId });
  }

  /**
   * 보냈던 '좋아요'를 취소합니다.
   */
  static async cancelLike(receiverId: number): Promise<ApiResponse<void>> {
    return api.delete<ApiResponse<void>>(`${API_ENDPOINTS.MATCHES}/${receiverId}`);
  }

  /**
   * 특정 사용자의 상세 정보를 조회합니다.
   */
  static async getMatchDetail(receiverId: number): Promise<any> {
    const response = await api.get<ApiResponse<any>>(`${API_ENDPOINTS.MATCHES}/candidates/${receiverId}`);
    return response.data?.data || response.data || response;
  }

  /**
   * 확정된 룸메이트 결과를 조회합니다.
   */
  static async getMatchResults(): Promise<ApiResponse<any>> {
    return api.get<ApiResponse<any>>(API_ENDPOINTS.MATCH_RESULTS);
  }

  /**
   * 매칭 요청을 최종 확정합니다.
   */
  static async confirmMatch(matchId: number): Promise<ApiResponse<any>> {
    return api.put<ApiResponse<any>>(`${API_ENDPOINTS.MATCHES}/${matchId}/confirm`, {
      action: 'accept'
    });
  }

  /**
   * 매칭 요청을 거절합니다.
   */
  static async rejectMatch(matchId: number): Promise<ApiResponse<any>> {
    return api.put<ApiResponse<any>>(`${API_ENDPOINTS.MATCHES}/${matchId}/confirm`, {
      action: 'reject'
    });
  }

  /**
   * 채팅방에 연결된 매칭 정보를 조회합니다.
   */
  static async getMatchByChatroom(chatroomId: number): Promise<ApiResponse<any>> {
    // 채팅방 상세 정보에서 user1Id, user2Id를 가져온 후
    // 매칭 상태 조회 API로 해당 매칭 정보를 찾습니다.
    return api.get<ApiResponse<any>>(API_ENDPOINTS.MATCH_STATUS);
  }

  /**
   * 사용자를 신고합니다.
   */
  static async reportUser(reportData: {
    reportedEmail: string;
    category: string;
    content: string;
  }): Promise<ApiResponse<any>> {
    return api.post<ApiResponse<any>>('/api/v1/report', reportData);
  }

  /**
   * 재매칭 요청
   */
  static async requestRematch(matchId: number): Promise<ApiResponse<any>> {
    return api.post<ApiResponse<any>>(`${API_ENDPOINTS.MATCHES}/${matchId}/rematch`);
  }
}

