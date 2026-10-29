import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  AdminDashboard,
  DashboardChartPoint,
  DashboardInstructorSession,
  DashboardStatusCount,
} from '../../core/dashboard/dashboard.models';
import { DashboardService } from '../../core/dashboard/dashboard.service';
import { DtCurrencyPipe } from '../../shared/pipes/dt-currency.pipe';
import { LocalDatePipe } from '../../shared/pipes/local-date.pipe';
import { AuthSession } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';

interface DashboardMetric {
  label: string;
  value: number;
  detail: string;
  icon: string;
  currency?: boolean;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, RouterLink, DtCurrencyPipe, LocalDatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  readonly session: AuthSession | null;
  readonly isAdmin: boolean;
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly metrics = signal<DashboardMetric[]>([]);
  readonly sessionsByDay = signal<DashboardChartPoint[]>([]);
  readonly paymentsByMonth = signal<DashboardChartPoint[]>([]);
  readonly sessionsByStatus = signal<DashboardStatusCount[]>([]);
  readonly todaySessions = signal<DashboardInstructorSession[]>([]);
  readonly upcomingSessions = signal<DashboardInstructorSession[]>([]);
  readonly taughtHours = signal(0);

  constructor(
    authService: AuthService,
    private readonly dashboardService: DashboardService,
  ) {
    this.session = authService.getSession();
    this.isAdmin = this.session?.roles.includes('ADMIN') ?? false;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    if (this.isAdmin) this.loadAdmin();
    else this.loadInstructor();
  }

  private loadAdmin(): void {
    this.dashboardService.getAdminDashboard()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => this.applyAdminDashboard(data),
        error: () => this.errorMessage.set('Le tableau de bord n’a pas pu être chargé. Vérifiez les accès et réessayez.'),
      });
  }

  private loadInstructor(): void {
    this.dashboardService.getInstructorDashboard()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => {
          this.todaySessions.set(data.todaySessions);
          this.upcomingSessions.set(data.upcomingSessions);
          this.taughtHours.set(data.hoursTaught.total);
          this.metrics.set([
            metric('Séances aujourd’hui', data.todaySessions.length, 'Planning du jour', 'today'),
            metric('Prochaines séances', data.upcomingSessions.length, '10 prochaines séances planifiées', 'event_available'),
            metric('Mes candidats', data.candidatesCount, 'Candidats qui vous sont attribués', 'groups'),
            metric(
              'Heures enseignées',
              data.hoursTaught.total,
              `${data.hoursTaught.currentMonth} h ce mois-ci`,
              'schedule',
            ),
          ]);
        },
        error: () => this.errorMessage.set('Le tableau de bord moniteur n’a pas pu être chargé. Réessayez.'),
      });
  }

  private applyAdminDashboard(data: AdminDashboard): void {
    this.metrics.set([
      metric('Candidats', data.candidatesCount, 'Profils enregistrés', 'groups'),
      metric('Moniteurs', data.instructorsCount, 'Équipe active', 'school'),
      metric('Séances aujourd’hui', data.sessionsToday, `${data.sessionsThisWeek} cette semaine`, 'today'),
      metric('Terminées', data.sessionsDone, 'Séances réalisées', 'task_alt'),
      metric(
        'Planifiées',
        data.sessionsPlanned,
        `${data.sessionsByStatus.find((item) => item.status === 'CANCELLED')?.value ?? 0} annulées`,
        'event',
      ),
      { ...metric('Total payé', data.totalPaid, 'Tous paiements enregistrés', 'payments'), currency: true },
      {
        ...metric('Reste à payer', data.totalRemaining, 'Soldes positifs des candidats', 'account_balance_wallet'),
        currency: true,
      },
    ]);
    this.sessionsByDay.set(data.sessionsPerDay);
    this.paymentsByMonth.set(data.paymentsPerMonth);
    this.sessionsByStatus.set(data.sessionsByStatus);
  }
}

function metric(label: string, value: number, detail: string, icon: string): DashboardMetric {
  return { label, value, detail, icon };
}
