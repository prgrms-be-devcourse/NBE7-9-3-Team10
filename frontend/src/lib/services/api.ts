import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { API_CONFIG, API_ENDPOINTS, ApiResponse, ApiError } from '@/types/api';

// Axios 인스턴스 생성
const createApiInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: API_CONFIG.BASE_URL,
    timeout: API_CONFIG.TIMEOUT,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // 요청 인터셉터 - JWT 토큰 자동 추가
  instance.interceptors.request.use(
    (config) => {
      // 클라이언트 사이드에서만 토큰 처리
      if (typeof window !== 'undefined') {
        // 관리자 토큰 확인
        const adminToken = localStorage.getItem('adminAccessToken');
        const isAdmin = localStorage.getItem('isAdmin');
        
        // 일반 사용자 토큰 확인
        const userToken = localStorage.getItem('accessToken');
        
        // 관리자로 로그인된 경우 관리자 토큰 사용
        if (isAdmin === 'true' && adminToken) {
          config.headers.Authorization = `Bearer ${adminToken}`;
        } 
        // 일반 사용자로 로그인된 경우 사용자 토큰 사용
        else if (userToken) {
          config.headers.Authorization = `Bearer ${userToken}`;
        }
      }
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // 응답 인터셉터 - 에러 처리 및 토큰 갱신
  instance.interceptors.response.use(
    (response: AxiosResponse) => {
      return response;
    },
    async (error: AxiosError) => {
      const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

      // 401 에러 처리 (토큰 만료)
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;

        // 현재 페이지가 로그인 페이지라면 리다이렉트하지 않음
        if (typeof window !== 'undefined' && 
            (window.location.pathname.includes('/login') || 
             window.location.pathname.includes('/admin/login'))) {
          return Promise.reject(error);
        }

        // 관리자인 경우
        const isAdmin = localStorage.getItem('isAdmin');
        if (isAdmin === 'true') {
          localStorage.removeItem('adminAccessToken');
          localStorage.removeItem('adminId');
          localStorage.removeItem('adminEmail');
          localStorage.removeItem('isAdmin');
          window.location.href = '/admin/login';
        } else {
          // 일반 사용자인 경우
          localStorage.removeItem('accessToken');
          window.location.href = '/login';
        }
      }

      let errorMessage = '알 수 없는 오류가 발생했습니다.';
      
      if (error.response?.data) {
        const responseData = error.response.data;
        // 다양한 형태의 에러 메시지 추출 시도
        if (typeof responseData === 'string') {
          errorMessage = responseData;
        } else if (typeof responseData === 'object') {
          errorMessage = (responseData as any).message || 
                        (responseData as any).error || 
                        (responseData as any).errorMessage ||
                        errorMessage;
        }
      } else if (error.message) {
        errorMessage = error.message;
      }

      const apiError: ApiError = {
        message: errorMessage,
        status: error.response?.status || 500,
        timestamp: new Date().toISOString(),
        path: error.config?.url,
        errorCode: (error.response?.data as any)?.errorCode,
      };

      return Promise.reject(apiError);
    }
  );

  return instance;
};

// API 인스턴스 생성
export const apiClient = createApiInstance();

// API 응답 래퍼 함수
export const apiRequest = async <T>(
  requestConfig: AxiosRequestConfig
): Promise<T> => {
  try {
    const response = await apiClient.request<T>(requestConfig);
    return response.data;
  } catch (error: unknown) {
    // 인터셉터에서 변환된 ApiError를 그대로 전달
    // 만약 ApiError가 아니면 변환
    if (error && typeof error === 'object' && 'message' in error) {
      throw error;
    }
    // 예상치 못한 에러 형태인 경우 ApiError로 변환
    throw {
      message: error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.',
      status: 500,
      timestamp: new Date().toISOString(),
      path: (requestConfig as any)?.url,
    } as ApiError;
  }
};

// HTTP 메서드별 헬퍼 함수들
export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    apiRequest<T>({ ...config, method: 'GET', url }),
  
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    apiRequest<T>({ ...config, method: 'POST', url, data }),
  
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    apiRequest<T>({ ...config, method: 'PUT', url, data }),
  
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    apiRequest<T>({ ...config, method: 'DELETE', url }),
  
  patch: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    apiRequest<T>({ ...config, method: 'PATCH', url, data }),
};

// API 엔드포인트 상수들 export
export { API_ENDPOINTS, API_CONFIG };

export default apiClient;