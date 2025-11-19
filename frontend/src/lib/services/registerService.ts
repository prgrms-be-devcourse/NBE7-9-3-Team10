import { api, API_ENDPOINTS } from '@/lib/services/api';
import { SignupRequest, SignupResponse } from '@/types/user';
import { MessageResponse } from '@/types/verification';

export interface SchoolInfo {
  schoolName: string;
  domain: string;
}

export class RegisterService {
  /**
   * 전체 학교 목록 조회
   * 프론트엔드의 자동완성 기능을 위해 1,892개 학교 정보 반환
   */
  static async getAllSchools(): Promise<SchoolInfo[]> {
    const response = await fetch('/api/v1/email/all-schools');
    
    if (!response.ok) {
      throw new Error('학교 목록을 불러올 수 없습니다.');
    }
    
    const data: SchoolInfo[] = await response.json();
    return data;
  }

  /**
   * 이메일 인증번호 요청
   */
  static async requestVerification(email: string): Promise<MessageResponse> {
    const res = await api.post<MessageResponse>(API_ENDPOINTS.EMAIL_REQUEST, { email });
    return res.data;
  }

  /**
   * 이메일 인증번호 검증
   */
  static async verifyEmailCode(email: string, code: string): Promise<MessageResponse> {
    const res = await api.post<MessageResponse>(API_ENDPOINTS.EMAIL_VERIFY, { email, code });
    return res.data;
  }

  /**
   * 회원가입
   */
  static async signup(data: SignupRequest): Promise<SignupResponse> {
    const res = await api.post<SignupResponse>(API_ENDPOINTS.SIGNUP, data);
    return res.data;
  }
}