import { api } from '@/lib/services/api';
import { API_ENDPOINTS } from '@/types/api';

export interface UserInfo {
  email: string;
  name: string;
  gender: 'MALE' | 'FEMALE';
  birthDate: string;
  university: string;
}

export interface EmailDomainInfo {
  university: string;
  domain: string;
  currentEmail: string;
}

export interface UserUpdateEmailResponse extends UserInfo {
  accessToken: string;
}

export class UserService {
  static async getUserInfo(): Promise<UserInfo> {
    const response = await api.get<UserInfo>(API_ENDPOINTS.USER);
    return response;
  }

  static async updateName(name: string): Promise<UserInfo> {
    const response = await api.patch<UserInfo>(
      `${API_ENDPOINTS.USER}/name`,
      { name }
    );
    return response;
  }

  /**
   * 현재 사용자의 대학교 도메인 조회
   */
  static async getEmailDomain(): Promise<EmailDomainInfo> {
    const response = await api.get<EmailDomainInfo>(
      `${API_ENDPOINTS.USER}/email/domain`
    );
    return response;
  }

  /**
   * 이메일 수정 (emailPrefix + code 사용)
   * @param emailPrefix - 이메일 앞 부분만 (예: "kim")
   * @param code - 인증 코드
   */
  static async updateEmail(
    emailPrefix: string,
    code: string
  ): Promise<UserUpdateEmailResponse> {
    const response = await api.patch<UserUpdateEmailResponse>(
      `${API_ENDPOINTS.USER}/email`,
      { emailPrefix, code }
    );
    return response;
  }
}