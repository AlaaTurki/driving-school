import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthSession } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { Candidate } from '../../core/candidates/candidate.models';
import { CandidateService } from '../../core/candidates/candidate.service';
import { Circuit } from '../../core/circuits/circuit.models';
import { CircuitService } from '../../core/circuits/circuit.service';
import { DrivingSessionService } from '../../core/driving-sessions/driving-session.service';
import { SaveDrivingSessionRequest } from '../../core/driving-sessions/driving-session.models';
import { Instructor } from '../../core/instructors/instructor.models';
import { InstructorService } from '../../core/instructors/instructor.service';
import {
  ScheduleAvailability,
  ScheduleDay,
  ScheduleEntry,
} from '../../core/schedule/schedule.models';
import { ScheduleService } from '../../core/schedule/schedule.service';
import { LocalTimePipe } from '../../shared/pipes/local-time.pipe';

interface CalendarDay {
  date: string;
  label: string;
  number: string;
  month: string;
  isToday: boolean;
  sessions: ScheduleEntry[];
}

function localDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function startOfWeek(date: Date): Date {
  const start = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const offset = (start.getDay() + 6) % 7;
  start.setDate(start.getDate() - offset);
  return start;
}

@Component({
  selector: 'app-schedule',
  standalone: true,
  imports: [FormsModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule, LocalTimePipe],
  templateUrl: './schedule.component.html',
  styleUrl: './schedule.component.scss',
})
export class ScheduleComponent {
  readonly session: AuthSession | null;
  readonly isCandidate: boolean;
  readonly isAdmin: boolean;
  readonly isInstructor: boolean;
  readonly days = signal<CalendarDay[]>([]);
  readonly instructors = signal<Instructor[]>([]);
  readonly selectedInstructorId = signal('');
  readonly loading = signal(true);
  readonly loadingInstructors = signal(false);
  readonly checkingAvailability = signal(false);
  readonly errorMessage = signal('');
  readonly availabilityError = signal('');
  readonly availability = signal<ScheduleAvailability | null>(null);
  readonly weekLabel = signal('');
  readonly weekStart = signal(startOfWeek(new Date()));
  readonly viewMode = signal<'week' | 'day'>('week');
  readonly selectedDate = signal(localDate(new Date()));
  readonly bookingOpen = signal(false);
  readonly bookingSaving = signal(false);
  readonly bookingError = signal('');
  readonly bookingSuccess = signal('');
  readonly candidates = signal<Candidate[]>([]);
  readonly circuits = signal<Circuit[]>([]);
  booking = { candidateId: '', instructorId: '', vehicleId: '', circuitId: '', price: null as number | null, notes: '' };
  readonly today = localDate(new Date());
  readonly scheduleCount = computed(() => this.days().reduce((count, day) => count + day.sessions.length, 0));
  availabilityDate = this.today;
  availabilityStart = '10:00';
  availabilityEnd = '12:00';

  constructor(
    private readonly authService: AuthService,
    private readonly scheduleService: ScheduleService,
    private readonly instructorService: InstructorService,
    private readonly router: Router,
    private readonly candidateService: CandidateService,
    private readonly circuitService: CircuitService,
    private readonly sessionService: DrivingSessionService,
  ) {
    this.session = this.authService.getSession();
    this.isCandidate = this.session?.roles.includes('CANDIDATE') ?? false;
    this.isAdmin = this.session?.roles.includes('ADMIN') ?? false;
    this.isInstructor = this.session?.roles.includes('INSTRUCTOR') ?? false;
    this.loadSchedule();
    if (this.isAdmin) this.loadInstructors();
    if (!this.isCandidate) this.loadBookingOptions();
  }

  loadSchedule(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    const weekStart = this.viewMode() === 'day' ? new Date(`${this.selectedDate()}T12:00:00`) : this.weekStart();
    const weekEnd = new Date(weekStart);
    if (this.viewMode() === 'week') weekEnd.setDate(weekEnd.getDate() + 6);
    const request = this.isCandidate
      ? this.scheduleService.getMySchedule()
      : this.scheduleService.getSchedule(localDate(weekStart), localDate(weekEnd), this.selectedInstructorId());
    request.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (schedule) => this.days.set(this.makeCalendar(schedule)),
      error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
    });
    this.weekLabel.set(this.viewMode() === 'day'
      ? new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(weekStart)
      : new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(weekStart));
  }

  changeWeek(offset: number): void {
    if (this.viewMode() === 'day') {
      const date = new Date(`${this.selectedDate()}T12:00:00`);
      date.setDate(date.getDate() + offset);
      this.selectedDate.set(localDate(date));
      this.weekStart.set(startOfWeek(date));
      this.loadSchedule();
      return;
    }
    const start = new Date(this.weekStart());
    start.setDate(start.getDate() + offset * 7);
    this.weekStart.set(start);
    this.loadSchedule();
  }

  goToCurrentWeek(): void {
    this.weekStart.set(startOfWeek(new Date()));
    this.selectedDate.set(this.today);
    this.loadSchedule();
  }

  setView(mode: 'week' | 'day'): void {
    this.viewMode.set(mode);
    this.loadSchedule();
  }

  onInstructorChange(event: Event): void {
    if (!(event.target instanceof HTMLSelectElement)) return;
    this.selectedInstructorId.set(event.target.value);
    this.loadSchedule();
  }

  setAvailabilityField(field: 'availabilityDate' | 'availabilityStart' | 'availabilityEnd', event: Event): void {
    const target = event.target;
    if (target instanceof HTMLInputElement) this[field] = target.value;
    this.availability.set(null);
    this.availabilityError.set('');
  }

  checkAvailability(): void {
    if (!this.availabilityDate || !this.availabilityStart || !this.availabilityEnd) {
      this.availabilityError.set('Renseignez la date et les deux horaires.');
      return;
    }
    if (this.availabilityEnd <= this.availabilityStart) {
      this.availabilityError.set('L’heure de fin doit être après l’heure de début.');
      return;
    }
    this.availabilityError.set('');
    this.availability.set(null);
    this.checkingAvailability.set(true);
    this.scheduleService.getAvailability(
      this.availabilityDate,
      this.availabilityStart,
      this.availabilityEnd,
    ).pipe(finalize(() => this.checkingAvailability.set(false))).subscribe({
      next: (result) => this.availability.set(result),
      error: (error: unknown) => this.availabilityError.set(this.errorText(error)),
    });
  }

  startBooking(): void {
    this.booking = {
      candidateId: '',
      instructorId: this.availability()?.instructors[0]?.id ?? '',
      vehicleId: this.availability()?.vehicles[0]?.id ?? '',
      circuitId: '',
      price: null,
      notes: '',
    };
    this.bookingError.set('');
    this.bookingSuccess.set('');
    this.bookingOpen.set(true);
  }

  onBookingCircuitChange(): void {
    const circuit = this.circuits().find((item) => item.id === this.booking.circuitId);
    if (circuit) this.booking.price = circuit.price;
  }

  saveBooking(): void {
    const availability = this.availability();
    if (!availability || !this.booking.candidateId) {
      this.bookingError.set('Sélectionnez un candidat.');
      return;
    }
    if (this.isAdmin && !this.booking.instructorId) {
      this.bookingError.set('Sélectionnez un moniteur disponible.');
      return;
    }
    if (!this.booking.circuitId && this.booking.price === null) {
      this.bookingError.set('Un prix est requis lorsqu’aucun circuit n’est sélectionné.');
      return;
    }
    const request: SaveDrivingSessionRequest = {
      candidateId: this.booking.candidateId,
      instructorId: this.isAdmin ? this.booking.instructorId : null,
      vehicleId: this.booking.vehicleId || null,
      circuitId: this.booking.circuitId || null,
      sessionDate: this.availabilityDate,
      startTime: this.availabilityStart,
      endTime: this.availabilityEnd,
      price: this.booking.price,
      notes: this.booking.notes.trim() || null,
    };
    this.bookingSaving.set(true);
    this.bookingError.set('');
    this.sessionService.create(request).pipe(finalize(() => this.bookingSaving.set(false))).subscribe({
      next: () => {
        this.bookingOpen.set(false);
        this.availability.set(null);
        this.bookingSuccess.set('La séance a été créée.');
        this.loadSchedule();
      },
      error: (error: unknown) => this.bookingError.set(this.errorText(error)),
    });
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  statusLabel(status: ScheduleEntry['status']): string {
    return {
      PLANNED: 'Planifiée',
      DONE: 'Effectuée',
      CANCELLED: 'Annulée',
      ABSENT: 'Absent',
    }[status];
  }

  private loadInstructors(): void {
    this.loadingInstructors.set(true);
    this.instructorService.findAll({ search: '', status: 'ACTIVE', page: 0, size: 100 })
      .pipe(finalize(() => this.loadingInstructors.set(false)))
      .subscribe({
        next: (page) => this.instructors.set(page.content),
        error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
      });
  }

  private loadBookingOptions(): void {
    this.candidateService.findAll({ search: '', status: 'ACTIVE', page: 0, size: 100 }).subscribe({
      next: (page) => this.candidates.set(page.content),
      error: (error: unknown) => this.bookingError.set(this.errorText(error)),
    });
    this.circuitService.findAll({ search: '', active: true, page: 0, size: 100 }).subscribe({
      next: (page) => this.circuits.set(page.content),
      error: (error: unknown) => this.bookingError.set(this.errorText(error)),
    });
  }

  private makeCalendar(schedule: ScheduleDay[]): CalendarDay[] {
    const scheduleByDate = new Map(schedule.map((day) => [day.date, day.sessions]));
    const start = this.isCandidate ? new Date() : this.viewMode() === 'day'
      ? new Date(`${this.selectedDate()}T12:00:00`)
      : this.weekStart();
    const days: CalendarDay[] = [];
    for (let index = 0; index < (this.isCandidate ? 0 : this.viewMode() === 'day' ? 1 : 7); index++) {
      const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + index);
      const key = localDate(date);
      days.push({
        date: key,
        label: new Intl.DateTimeFormat('fr-FR', { weekday: 'long' }).format(date),
        number: new Intl.DateTimeFormat('fr-FR', { day: '2-digit' }).format(date),
        month: new Intl.DateTimeFormat('fr-FR', { month: 'short' }).format(date),
        isToday: key === this.today,
        sessions: scheduleByDate.get(key) ?? [],
      });
    }
    if (this.isCandidate) {
      for (const [date, sessions] of scheduleByDate) {
        days.push({
          date,
          label: new Intl.DateTimeFormat('fr-FR', { weekday: 'long' }).format(new Date(`${date}T12:00:00`)),
          number: new Intl.DateTimeFormat('fr-FR', { day: '2-digit' }).format(new Date(`${date}T12:00:00`)),
          month: new Intl.DateTimeFormat('fr-FR', { month: 'short' }).format(new Date(`${date}T12:00:00`)),
          isToday: date === this.today,
          sessions,
        });
      }
    }
    return days;
  }

  private errorText(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires pour consulter ce planning.';
    }
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return 'Les dates ou horaires renseignés ne sont pas valides.';
    }
    return 'Le planning n’a pas pu être chargé. Veuillez réessayer.';
  }
}
