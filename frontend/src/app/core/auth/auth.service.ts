import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, finalize, shareReplay, tap, throwError } from 'rxjs';
import { AuthSession, LoginRequest, LoginResponse, RegistrationRequest } from './auth.models';

const SESSION_KEY = 'driving-school-auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private refreshRequest: Observable<LoginResponse> | null = null;

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

  refreshSession(): Observable<LoginResponse> {
    if (this.refreshRequest) {
      return this.refreshRequest;
    }

    const session = this.getSession();
    if (!session) {
      return throwError(() => new Error('No active session to refresh'));
    }

    let sharedRequest: Observable<LoginResponse>;
    sharedRequest = this.http.post<LoginResponse>('/api/auth/refresh', {
      refreshToken: session.refreshToken,
    }).pipe(
      tap((nextSession) => sessionStorage.setItem(SESSION_KEY, JSON.stringify(nextSession))),
      catchError((error: unknown) => {
        this.logout();
        return throwError(() => error);
      }),
      finalize(() => {
        if (this.refreshRequest === sharedRequest) {
          this.refreshRequest = null;
        }
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.refreshRequest = sharedRequest;
    return sharedRequest;
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
        !Number.isFinite(Date.parse(session.expiresAt))
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
