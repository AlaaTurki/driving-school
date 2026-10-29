import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let router: Router;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getSession']);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: { createUrlTree: jasmine.createSpy('createUrlTree').and.callFake((path) => path) } },
      ],
    });
    router = TestBed.inject(Router);
  });

  function runGuard(...roles: string[]) {
    const guard = roleGuard(...roles);
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
  }

  it('allows a user with any configured role', () => {
    authService.getSession.and.returnValue({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresAt: new Date().toISOString(),
      userId: 'user-id',
      email: 'instructor@example.com',
      roles: ['INSTRUCTOR'],
    });

    expect(runGuard('ADMIN', 'INSTRUCTOR')).toBe(true);
  });

  it('redirects candidates to the mobile-app notice', () => {
    authService.getSession.and.returnValue({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresAt: new Date().toISOString(),
      userId: 'candidate-id',
      email: 'candidate@example.com',
      roles: ['CANDIDATE'],
    });

    expect(runGuard('ADMIN')).not.toBe(true);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/mobile-app']);
  });

  it('redirects users without a session to login', () => {
    authService.getSession.and.returnValue(null);

    expect(runGuard('ADMIN')).not.toBe(true);
  });
});
