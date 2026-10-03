import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('posts credentials and stores the returned session for the browser tab', () => {
    const credentials = { email: 'admin@example.com', password: 'strong-password' };
    const response = {
      accessToken: 'signed-token',
      tokenType: 'Bearer' as const,
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      userId: '90fd6f8b-28c7-4f7d-9428-90d7d177664e',
      email: credentials.email,
      roles: ['ADMIN'],
    };

    service.login(credentials).subscribe((result) => {
      expect(result).toEqual(response);
      expect(service.getAccessToken()).toBe('signed-token');
      expect(service.getSession()?.roles).toEqual(['ADMIN']);
    });

    const request = httpTesting.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(credentials);
    request.flush(response);
  });

  it('registers a candidate and stores the returned session for the browser tab', () => {
    const registration = {
      fullName: 'Candidate Name',
      email: 'candidate@example.com',
      phone: '+216 12 345 678',
      password: 'strong-password',
    };
    const response = {
      accessToken: 'candidate-token',
      tokenType: 'Bearer' as const,
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      userId: '90fd6f8b-28c7-4f7d-9428-90d7d177664e',
      email: registration.email,
      roles: ['CANDIDATE'],
    };

    service.register(registration).subscribe((result) => {
      expect(result).toEqual(response);
      expect(service.getAccessToken()).toBe('candidate-token');
      expect(service.getSession()?.roles).toEqual(['CANDIDATE']);
    });

    const request = httpTesting.expectOne('/api/auth/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(registration);
    request.flush(response);
  });

  it('discards expired sessions', () => {
    sessionStorage.setItem('driving-school-auth', JSON.stringify({
      accessToken: 'expired-token',
      expiresAt: new Date(Date.now() - 60_000).toISOString(),
    }));

    expect(service.getAccessToken()).toBeNull();
    expect(sessionStorage.getItem('driving-school-auth')).toBeNull();
  });
});
