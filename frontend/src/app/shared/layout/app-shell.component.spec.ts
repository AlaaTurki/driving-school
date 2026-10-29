import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { AppShellComponent } from './app-shell.component';

describe('AppShellComponent navigation', () => {
  function createShell(roles: string[]): AppShellComponent {
    TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            getSession: () => ({
              accessToken: 'access',
              refreshToken: 'refresh',
              tokenType: 'Bearer',
              expiresAt: new Date(Date.now() + 60_000).toISOString(),
              userId: 'user-id',
              email: 'user@example.com',
              roles,
            }),
            logout: () => undefined,
          },
        },
      ],
    });

    return TestBed.createComponent(AppShellComponent).componentInstance;
  }

  it('shows administration modules only to administrators', () => {
    const shell = createShell(['ADMIN']);
    const labels = shell.visibleMenu().map((item) => item.label);

    expect(labels).toContain('Candidates');
    expect(labels).toContain('Instructors');
    expect(labels).toContain('Payments');
    expect(labels).not.toContain('Mes candidats');
  });

  it('shows instructor navigation with read-only payments but without administration-only modules', () => {
    const shell = createShell(['INSTRUCTOR']);
    const labels = shell.visibleMenu().map((item) => item.label);

    expect(labels).toContain('Mes candidats');
    expect(labels).toContain('Sessions');
    expect(labels).toContain('Schedule');
    expect(labels).toContain('Payments');
    expect(labels).not.toContain('Instructors');
  });
});
