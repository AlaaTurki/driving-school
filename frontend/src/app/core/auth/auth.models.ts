export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegistrationRequest {
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
  userId: string;
  email: string;
  roles: string[];
}

export type AuthSession = LoginResponse;
