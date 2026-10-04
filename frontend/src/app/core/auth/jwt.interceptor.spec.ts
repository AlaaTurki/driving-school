import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { jwtInterceptor } from './jwt.interceptor';

describe('jwtInterceptor', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('refreshes an expired access token and retries the original request', () => {
    sessionStorage.setItem('driving-school-auth', JSON.stringify({
      accessToken: 'expired-access-token',
      refreshToken: 'current-refresh-token',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() - 60_000).toISOString(),
      userId: 'user-id',
      email: 'admin@example.com',
      roles: ['ADMIN'],
    }));

    let response: unknown;
    TestBed.inject(HttpClient).get('/api/vehicles?page=0&size=20').subscribe((value) => {
      response = value;
    });

    const initialRequest = httpTesting.expectOne('/api/vehicles?page=0&size=20');
    expect(initialRequest.request.headers.get('Authorization')).toBe('Bearer expired-access-token');
    initialRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    const refreshRequest = httpTesting.expectOne('/api/auth/refresh');
    expect(refreshRequest.request.body).toEqual({ refreshToken: 'current-refresh-token' });
    refreshRequest.flush({
      accessToken: 'fresh-access-token',
      refreshToken: 'rotated-refresh-token',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      userId: 'user-id',
      email: 'admin@example.com',
      roles: ['ADMIN'],
    });

    const retriedRequest = httpTesting.expectOne('/api/vehicles?page=0&size=20');
    expect(retriedRequest.request.headers.get('Authorization')).toBe('Bearer fresh-access-token');
    retriedRequest.flush({ content: [] });
    expect(response).toEqual({ content: [] });
  });
});
