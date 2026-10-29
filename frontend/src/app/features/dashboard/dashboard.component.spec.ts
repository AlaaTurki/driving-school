import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let httpMock: HttpTestingController;
  let roles: string[];

  beforeEach(() => {
    roles = ['ADMIN'];
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            getSession: () => ({
              accessToken: 'test',
              refreshToken: 'test',
              tokenType: 'Bearer',
              expiresAt: '',
              userId: 'admin-id',
              email: 'admin@example.com',
              roles,
            }),
          },
        },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads admin metrics and charts with one backend request', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    const request = httpMock.expectOne('/api/dashboard');
    expect(request.request.method).toBe('GET');
    request.flush({
      candidatesCount: 12,
      instructorsCount: 3,
      sessionsToday: 2,
      sessionsThisWeek: 9,
      sessionsDone: 6,
      sessionsPlanned: 4,
      totalPaid: 320,
      totalRemaining: 45,
      sessionsPerDay: [{ label: '16 oct.', value: 2, height: 100 }],
      paymentsPerMonth: [{ label: 'mai', value: 50, height: 100 }],
      sessionsByStatus: [
        { status: 'PLANNED', label: 'Planifiées', value: 4, height: 67 },
        { status: 'DONE', label: 'Terminées', value: 6, height: 100 },
        { status: 'ABSENT', label: 'Absents', value: 2, height: 33 },
        { status: 'CANCELLED', label: 'Annulées', value: 1, height: 17 },
      ],
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.metrics().map((item) => item.value)).toContain(45);
    expect(fixture.componentInstance.sessionsByDay().length).toBe(1);
    expect(fixture.componentInstance.paymentsByMonth().length).toBe(1);
    expect(fixture.componentInstance.sessionsByStatus().length).toBe(4);
    expect(fixture.nativeElement.textContent).toContain('Séances par jour');
    expect(fixture.nativeElement.textContent).toContain('Paiements par mois');
    expect(fixture.nativeElement.textContent).toContain('Répartition par statut');
  });

  it('loads instructor dashboard data with one backend request', () => {
    roles = ['INSTRUCTOR'];
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    const request = httpMock.expectOne('/api/dashboard/instructor');
    expect(request.request.method).toBe('GET');
    request.flush({
      todaySessions: [],
      upcomingSessions: [],
      candidatesCount: 5,
      hoursTaught: { total: 14.5, currentMonth: 3.5 },
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.taughtHours()).toBe(14.5);
    expect(fixture.nativeElement.textContent).toContain('14.5');
    expect(fixture.nativeElement.textContent).toContain('3.5 h ce mois-ci');
    expect(fixture.nativeElement.textContent).toContain('Mes séances du jour');
  });
});
