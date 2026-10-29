import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize, forkJoin, of } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Candidate } from '../../core/candidates/candidate.models';
import { CandidateService } from '../../core/candidates/candidate.service';
import { Circuit } from '../../core/circuits/circuit.models';
import { CircuitService } from '../../core/circuits/circuit.service';
import {
  DrivingSession,
  DrivingSessionPage,
  DrivingSessionQuery,
  DrivingSessionStatus,
  SaveDrivingSessionRequest,
} from '../../core/driving-sessions/driving-session.models';
import { DrivingSessionService } from '../../core/driving-sessions/driving-session.service';
import { StatementPage } from '../../core/financials/financial.models';
import { FinancialService } from '../../core/financials/financial.service';
import { Instructor } from '../../core/instructors/instructor.models';
import { InstructorService } from '../../core/instructors/instructor.service';
import { Vehicle } from '../../core/vehicles/vehicle.models';
import { VehicleService } from '../../core/vehicles/vehicle.service';
import { DtCurrencyPipe } from '../../shared/pipes/dt-currency.pipe';
import { LocalDatePipe } from '../../shared/pipes/local-date.pipe';
import { LocalTimePipe } from '../../shared/pipes/local-time.pipe';
import { AuthService } from '../../core/auth/auth.service';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-sessions',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    DtCurrencyPipe,
    LocalDatePipe,
    LocalTimePipe,
  ],
  templateUrl: './sessions.component.html',
  styleUrl: './sessions.component.scss',
})
export class SessionsComponent {
  readonly isAdmin: boolean;
  readonly sessions = signal<DrivingSession[]>([]);
  readonly candidates = signal<Candidate[]>([]);
  readonly instructors = signal<Instructor[]>([]);
  readonly vehicles = signal<Vehicle[]>([]);
  readonly circuits = signal<Circuit[]>([]);
  readonly totalElements = signal(0);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(false);
  readonly loadingOptions = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly errorMessage = signal('');
  readonly formConflicts = signal<string[]>([]);
  readonly successMessage = signal('');
  readonly formOpen = signal(false);
  readonly statuses: DrivingSessionStatus[] = ['PLANNED', 'DONE', 'CANCELLED', 'ABSENT'];
  readonly filters = {
    dateFrom: '',
    dateTo: '',
    candidateId: '',
    instructorId: '',
    status: '' as DrivingSessionStatus | '',
    circuitId: '',
  };
  readonly draft = this.emptyDraft();

  constructor(
    auth: AuthService,
    private readonly sessionService: DrivingSessionService,
    private readonly candidateService: CandidateService,
    private readonly instructorService: InstructorService,
    private readonly vehicleService: VehicleService,
    private readonly circuitService: CircuitService,
    private readonly financialService: FinancialService,
    route: ActivatedRoute,
  ) {
    this.isAdmin = auth.getSession()?.roles.includes('ADMIN') ?? false;
    this.filters.candidateId = route.snapshot.queryParamMap.get('candidateId') ?? '';
    this.loadOptions();
    this.load();
  }

  load(page = this.page()): void {
    if (this.filters.dateFrom && this.filters.dateTo && this.filters.dateTo < this.filters.dateFrom) {
      this.errorMessage.set('La date de fin doit être postérieure ou égale à la date de début.');
      return;
    }
    this.errorMessage.set('');
    this.loading.set(true);
    this.page.set(page);
    this.sessionService.findAll(this.query(page))
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.totalElements.set(result.totalElements);
          this.totalPages.set(result.totalPages);
          this.attachPayments(result);
        },
        error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
      });
  }

  applyFilters(): void {
    this.load(0);
  }

  startCreate(): void {
    this.editingId.set(null);
    this.formConflicts.set([]);
    Object.assign(this.draft, this.emptyDraft());
    this.draft.candidateId = this.filters.candidateId;
    this.formOpen.set(true);
    this.errorMessage.set('');
  }

  startEdit(session: DrivingSession): void {
    this.editingId.set(session.id);
    this.formConflicts.set([]);
    Object.assign(this.draft, {
      candidateId: session.candidate.id,
      instructorId: session.instructor.id,
      vehicleId: session.vehicle?.id ?? '',
      circuitId: session.circuit?.id ?? '',
      sessionDate: session.sessionDate,
      startTime: session.startTime.slice(0, 5),
      endTime: session.endTime.slice(0, 5),
      price: session.price,
      notes: session.notes ?? '',
    });
    this.formOpen.set(true);
    this.errorMessage.set('');
  }

  onCircuitChange(): void {
    const circuit = this.circuits().find((item) => item.id === this.draft.circuitId);
    if (circuit) this.draft.price = circuit.price;
  }

  save(): void {
    this.formConflicts.set([]);
    if (this.saving() || !this.draft.candidateId || !this.draft.sessionDate
      || !this.draft.startTime || !this.draft.endTime || this.draft.endTime <= this.draft.startTime) {
      this.errorMessage.set('Renseignez le candidat, la date et un horaire valide.');
      return;
    }
    if (this.isAdmin && !this.draft.instructorId) {
      this.errorMessage.set('Sélectionnez un moniteur.');
      return;
    }
    if (this.draft.price !== null && this.draft.price < 0) {
      this.errorMessage.set('Le prix ne peut pas être négatif.');
      return;
    }
    if (!this.editingId() && !this.draft.circuitId && this.draft.price === null) {
      this.errorMessage.set('Un prix est requis lorsqu’aucun circuit n’est sélectionné.');
      return;
    }
    const request: SaveDrivingSessionRequest = {
      candidateId: this.draft.candidateId,
      instructorId: this.isAdmin ? this.draft.instructorId : this.editingId()
        ? this.draft.instructorId
        : null,
      vehicleId: this.draft.vehicleId || null,
      circuitId: this.draft.circuitId || null,
      sessionDate: this.draft.sessionDate,
      startTime: this.draft.startTime,
      endTime: this.draft.endTime,
      notes: this.draft.notes.trim() || null,
    };
    const id = this.editingId();
    if (!id) request.price = this.draft.price === null ? null : Number(this.draft.price);
    this.saving.set(true);
    this.errorMessage.set('');
    const operation = id ? this.sessionService.update(id, request) : this.sessionService.create(request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.successMessage.set(id ? 'La séance a été modifiée.' : 'La séance a été créée.');
        this.formOpen.set(false);
        this.load(this.page());
      },
      error: (error: unknown) => {
        const message = this.errorText(error);
        this.errorMessage.set(message);
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.formConflicts.set(message.split('\n').map((line) => line.trim()).filter(Boolean));
          this.scrollToConflicts();
        }
      },
    });
  }

  changeStatus(session: DrivingSession, status: DrivingSessionStatus): void {
    const note = window.prompt('Remarque (facultative) :', session.notes ?? '');
    if (note === null || this.saving()) return;
    this.saving.set(true);
    this.errorMessage.set('');
    this.sessionService.changeStatus(session.id, { status, notes: note.trim() || null })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set(`Statut mis à jour : ${this.statusLabel(status)}.`);
          this.load(this.page());
        },
        error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
      });
  }

  delete(session: DrivingSession): void {
    if (!this.isAdmin || this.saving()
      || !window.confirm(`Supprimer la séance du ${session.sessionDate} à ${session.startTime.slice(0, 5)} ?`)) return;
    this.saving.set(true);
    this.errorMessage.set('');
    this.sessionService.delete(session.id).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.successMessage.set('La séance a été supprimée.');
        this.load(this.page());
      },
      error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
    });
  }

  statusLabel(status: DrivingSessionStatus): string {
    return { PLANNED: 'Planifiée', DONE: 'Effectuée', CANCELLED: 'Annulée', ABSENT: 'Absent' }[status];
  }

  weekdayLabel(date: string): string {
    return new Intl.DateTimeFormat('fr-FR', { weekday: 'long' }).format(new Date(`${date}T12:00:00`));
  }

  changePage(offset: number): void {
    const next = this.page() + offset;
    if (next >= 0 && next < this.totalPages() && !this.loading()) this.load(next);
  }

  private loadOptions(): void {
    this.loadingOptions.set(true);
    const optionRequests = {
      candidates: this.candidateService.findAll({ search: '', status: 'ACTIVE', page: 0, size: 100 }),
      vehicles: this.vehicleService.findAll({ search: '', active: true, page: 0, size: 100 }),
      circuits: this.circuitService.findAll({ search: '', active: true, page: 0, size: 100 }),
      instructors: this.isAdmin
        ? this.instructorService.findAll({ search: '', status: 'ACTIVE', page: 0, size: 100 })
        : of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100, first: true, last: true }),
    };
    forkJoin(optionRequests).pipe(finalize(() => this.loadingOptions.set(false))).subscribe({
      next: (result) => {
        this.candidates.set(result.candidates.content);
        this.vehicles.set(result.vehicles.content);
        this.circuits.set(result.circuits.content);
        this.instructors.set(result.instructors.content);
      },
      error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
    });
  }

  private query(page: number): DrivingSessionQuery {
    return {
      dateFrom: this.filters.dateFrom || undefined,
      dateTo: this.filters.dateTo || undefined,
      candidateId: this.filters.candidateId || undefined,
      instructorId: this.isAdmin ? this.filters.instructorId || undefined : undefined,
      status: this.filters.status || undefined,
      circuitId: this.filters.circuitId || undefined,
      page,
      size: PAGE_SIZE,
    };
  }

  private attachPayments(page: DrivingSessionPage): void {
    const sessions = page.content;
    if (!sessions.length) {
      this.sessions.set([]);
      return;
    }
    const candidateIds = [...new Set(sessions.map((session) => session.candidate.id))];
    const dates = sessions.map((session) => session.sessionDate).sort();
    const statements = candidateIds.map((candidateId) =>
      this.financialService.getStatement({
        candidateId, dateFrom: dates[0], dateTo: dates[dates.length - 1], page: 0, size: 100,
      }));
    this.loading.set(true);
    forkJoin(statements).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (results) => {
        const bySession: Record<string, number> = {};
        for (const statement of results) {
          for (const line of statement.content) {
            bySession[line.sessionId] = line.paidAmount;
          }
        }
        this.sessions.set(sessions.map((session) => {
          const paidAmount = bySession[session.id];
          return paidAmount === undefined
            ? session
            : { ...session, paidAmount, remaining: session.price - paidAmount };
        }));
      },
      error: (error: unknown) => {
        this.sessions.set(sessions);
        this.errorMessage.set(this.errorText(error));
      },
    });
  }

  private emptyDraft(): {
    candidateId: string; instructorId: string; vehicleId: string; circuitId: string;
    sessionDate: string; startTime: string; endTime: string; price: number | null; notes: string;
  } {
    const today = new Date();
    const date = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    return {
      candidateId: '', instructorId: '', vehicleId: '', circuitId: '',
      sessionDate: date, startTime: '09:00', endTime: '10:00', price: null, notes: '',
    };
  }

  private scrollToConflicts(): void {
    window.setTimeout(() => {
      document.getElementById('conflict-alert')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  }

  private errorText(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) return 'Une erreur inattendue est survenue.';
    if (error.status === 0) return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    if (error.status === 409) {
      const body = error.error as { message?: unknown; conflicts?: unknown };
      const message = typeof body?.message === 'string' ? body.message : '';
      return message || 'Conflit de planning : le moniteur, le véhicule ou le candidat est déjà pris sur ce créneau.';
    }
    if (error.status === 400) {
      const body = error.error as { message?: unknown };
      return typeof body?.message === 'string' ? body.message : 'Vérifiez les dates, horaires et le tarif.';
    }
    if (error.status === 403) return 'Vous ne disposez pas des droits nécessaires pour cette opération.';
    if (error.status === 404) return 'La séance, le candidat ou la ressource demandée est introuvable.';
    return 'Les séances n’ont pas pu être chargées. Veuillez réessayer.';
  }
}
