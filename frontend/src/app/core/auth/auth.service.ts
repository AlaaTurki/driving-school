import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthSession, LoginRequest, LoginResponse, RegistrationRequest } from './auth.models';

const SESSION_KEY = 'driving-school-auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private readonly http: HttpClient) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', credentials).pipe(
      tap((session) => sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))),
    );
  }

  register(request: RegistrationRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/register', request).pipe(
      tap((session) => sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))),
    );
  }

  getSession(): AuthSession | null {
    const serialized = sessionStorage.getItem(SESSION_KEY);
    if (!serialized) {
      return null;
    }

    try {
      const session = JSON.parse(serialized) as AuthSession;
      if (
        !session.accessToken ||
        !session.refreshToken ||
        !session.expiresAt ||
        Date.parse(session.expiresAt) <= Date.now()
      ) {
        this.logout();
        return null;
      }
      return session;
    } catch {
      this.logout();
      return null;
    }
  }

  getAccessToken(): string | null {
    return this.getSession()?.accessToken ?? null;
  }

  logout(): void {
    sessionStorage.removeItem(SESSION_KEY);
  }
}
