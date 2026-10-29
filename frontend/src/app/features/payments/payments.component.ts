import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Observable, expand, finalize, forkJoin, map, reduce } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { Candidate } from '../../core/candidates/candidate.models';
import { CandidateService } from '../../core/candidates/candidate.service';
import {
  BalanceSummary,
  DrivingSessionStatus,
  Payment,
  PaymentMethod,
  PaymentPage,
  PaymentQuery,
  SavePaymentRequest,
  StatementPage,
} from '../../core/financials/financial.models';
import { FinancialService } from '../../core/financials/financial.service';
import { DtCurrencyPipe } from '../../shared/pipes/dt-currency.pipe';
import { LocalDatePipe } from '../../shared/pipes/local-date.pipe';

interface PaymentDraft {
  amount: number | null;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  notes: string;
}

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [
    DtCurrencyPipe,
    LocalDatePipe,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './payments.component.html',
  styleUrl: './payments.component.scss',
})
export class PaymentsComponent {
  readonly candidates = signal<Candidate[]>([]);
  readonly selectedCandidateId = signal('');
  readonly balance = signal<BalanceSummary | null>(null);
  readonly payments = signal<Payment[]>([]);
  readonly statement = signal<StatementPage | null>(null);
  readonly loadingCandidates = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly editingPaymentId = signal<string | null>(null);
  readonly isCandidate: boolean;
  readonly isAdmin: boolean;
  readonly paymentMethods: PaymentMethod[] = ['CASH', 'CARD', 'BANK_TRANSFER', 'OTHER'];
  readonly statementStatuses: DrivingSessionStatus[] = ['PLANNED', 'DONE', 'CANCELLED', 'ABSENT'];
  readonly pageSize = 20;
  readonly paymentPageNumber = signal(0);
  readonly paymentTotalPages = signal(0);
  readonly statementPageNumber = signal(0);
  readonly statementTotalPages = signal(0);
  dateFrom = '';
  dateTo = '';
  paymentDateFrom = '';
  paymentDateTo = '';
  paymentMethodFilter: PaymentMethod | '' = '';
  paymentCandidateId = '';
  statusFilter: DrivingSessionStatus | '' = '';
  readonly paymentDraft: PaymentDraft = this.emptyDraft();

  constructor(
    private readonly authService: AuthService,
    private readonly candidateService: CandidateService,
    private readonly financialService: FinancialService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {
    const roles = this.authService.getSession()?.roles ?? [];
    this.isCandidate = roles.includes('CANDIDATE');
    this.isAdmin = roles.includes('ADMIN');
    if (this.isCandidate) {
      this.loadFinancialData(0, 0);
    } else {
      this.loadCandidates();
    }
  }

  get selectedCandidate(): Candidate | undefined {
    return this.candidates().find((candidate) => candidate.id === this.selectedCandidateId());
  }

  loadCandidates(): void {
    this.errorMessage.set('');
    this.loadingCandidates.set(true);
    this.candidateService.findAll({ search: '', status: 'ALL', page: 0, size: 100 })
      .pipe(
        expand((page) => page.number + 1 < page.totalPages
          ? this.candidateService.findAll({
            search: '',
            status: 'ALL',
            page: page.number + 1,
            size: 100,
          })
          : EMPTY),
        reduce((all, page) => [...all, ...page.content], [] as Candidate[]),
        finalize(() => this.loadingCandidates.set(false)),
      )
      .subscribe({
        next: (candidates) => {
          this.candidates.set(candidates);
          if (candidates.length) {
            const requestedId = this.route.snapshot.queryParamMap.get('candidateId');
            this.selectedCandidateId.set(candidates.find((candidate) => candidate.id === requestedId)?.id ?? candidates[0].id);
            this.paymentCandidateId = requestedId && candidates.some((candidate) => candidate.id === requestedId)
              ? requestedId
              : '';
            this.loadFinancialData(0, 0);
          }
        },
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  onCandidateChange(event: Event): void {
    if (!(event.target instanceof HTMLSelectElement)) return;
    this.selectedCandidateId.set(event.target.value);
    this.resetPaymentForm();
    this.loadFinancialData(0, 0);
  }

  loadFinancialData(paymentPage = this.paymentPageNumber(), statementPage = this.statementPageNumber()): void {
    const candidateId = this.isCandidate ? undefined : this.selectedCandidateId();
    if (!this.isCandidate && !candidateId) return;

    this.errorMessage.set('');
    this.loading.set(true);
    if (this.paymentDateFrom && this.paymentDateTo && this.paymentDateTo < this.paymentDateFrom) {
      this.errorMessage.set('La date de fin du filtre de paiements doit être postérieure ou égale à la date de début.');
      return;
    }
    const paymentQuery: PaymentQuery = {
      candidateId: this.paymentCandidateId || undefined,
      dateFrom: this.paymentDateFrom || undefined,
      dateTo: this.paymentDateTo || undefined,
      paymentMethod: this.paymentMethodFilter || undefined,
      page: paymentPage,
      size: this.pageSize,
    };
    const paymentRequest = this.isAdmin
      ? this.financialService.getAllPayments(paymentQuery)
      : this.getScopedPayments(candidateId, paymentPage);
    forkJoin({
      balance: this.financialService.getBalance(candidateId),
      payments: paymentRequest,
      statement: this.financialService.getStatement({
        candidateId,
        dateFrom: this.dateFrom || undefined,
        dateTo: this.dateTo || undefined,
        status: this.statusFilter || undefined,
        page: statementPage,
        size: this.pageSize,
      }),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.balance.set(result.balance);
          this.payments.set(result.payments.content);
          this.paymentPageNumber.set(result.payments.number);
          this.paymentTotalPages.set(result.payments.totalPages);
          this.statement.set(result.statement);
          this.statementPageNumber.set(result.statement.number);
          this.statementTotalPages.set(result.statement.totalPages);
        },
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  applyStatementFilters(): void {
    if (this.dateFrom && this.dateTo && this.dateTo < this.dateFrom) {
      this.errorMessage.set('La date de fin doit être postérieure ou égale à la date de début.');
      return;
    }
    this.loadFinancialData(this.paymentPageNumber(), 0);
  }

  applyPaymentFilters(): void {
    this.loadFinancialData(0, this.statementPageNumber());
  }

  changePaymentPage(page: number): void {
    if (page >= 0 && page < this.paymentTotalPages() && !this.loading()) {
      this.loadFinancialData(page, this.statementPageNumber());
    }
  }

  changeStatementPage(page: number): void {
    if (page >= 0 && page < this.statementTotalPages() && !this.loading()) {
      this.loadFinancialData(this.paymentPageNumber(), page);
    }
  }

  editPayment(payment: Payment): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.editingPaymentId.set(payment.id);
    Object.assign(this.paymentDraft, {
      amount: payment.amount,
      paymentDate: payment.paymentDate,
      paymentMethod: payment.paymentMethod,
      notes: payment.notes ?? '',
    });
  }

  cancelPaymentEdit(): void {
    this.resetPaymentForm();
  }

  savePayment(): void {
    const candidateId = this.selectedCandidateId();
    const amount = this.paymentDraft.amount;
    if (!this.isAdmin || !candidateId || amount === null || amount <= 0 || this.saving()) return;

    const request: SavePaymentRequest = {
      candidateId,
      amount,
      paymentDate: this.paymentDraft.paymentDate,
      paymentMethod: this.paymentDraft.paymentMethod,
      notes: this.paymentDraft.notes.trim() || null,
    };
    const paymentId = this.editingPaymentId();
    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    const saveRequest = paymentId
      ? this.financialService.updatePayment(paymentId, request)
      : this.financialService.createPayment(request);
    saveRequest.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.successMessage.set(paymentId ? 'Le paiement a été modifié.' : 'Le paiement a été enregistré.');
        this.resetPaymentForm();
        this.loadFinancialData(0, this.statementPageNumber());
      },
      error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
    });
  }

  deletePayment(payment: Payment): void {
    if (!this.isAdmin || this.saving()) return;
    if (!window.confirm(`Supprimer le paiement de ${this.formatAmount(payment.amount)} du ${payment.paymentDate} ?`)) {
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    this.financialService.deletePayment(payment.id)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set('Le paiement a été supprimé.');
          this.loadFinancialData(this.paymentPageNumber(), this.statementPageNumber());
        },
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  paymentMethodLabel(method: PaymentMethod): string {
    switch (method) {
      case 'CASH': return 'Espèces';
      case 'CARD': return 'Carte';
      case 'BANK_TRANSFER': return 'Virement';
      case 'OTHER': return 'Autre';
    }
  }

  sessionStatusLabel(status: DrivingSessionStatus): string {
    switch (status) {
      case 'PLANNED': return 'Prévue';
      case 'DONE': return 'Effectuée';
      case 'CANCELLED': return 'Annulée';
      case 'ABSENT': return 'Absent';
    }
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'TND', minimumFractionDigits: 3 }).format(amount);
  }

  private resetPaymentForm(): void {
    this.editingPaymentId.set(null);
    Object.assign(this.paymentDraft, this.emptyDraft());
  }

  private getScopedPayments(candidateId: string | undefined, pageNumber: number): Observable<PaymentPage> {
    const hasFilters = !!(this.paymentDateFrom || this.paymentDateTo || this.paymentMethodFilter);
    if (!candidateId || !hasFilters) {
      return this.financialService.getPayments(candidateId, pageNumber, this.pageSize);
    }

    return this.financialService.getPayments(candidateId, 0, 100).pipe(
      expand((page) => page.number + 1 < page.totalPages
        ? this.financialService.getPayments(candidateId, page.number + 1, 100)
        : EMPTY),
      reduce((all, page) => [...all, ...page.content], [] as Payment[]),
      map((all) => {
        const filtered = all.filter((payment) =>
          (!this.paymentDateFrom || payment.paymentDate >= this.paymentDateFrom)
          && (!this.paymentDateTo || payment.paymentDate <= this.paymentDateTo)
          && (!this.paymentMethodFilter || payment.paymentMethod === this.paymentMethodFilter));
        const start = pageNumber * this.pageSize;
        const content = filtered.slice(start, start + this.pageSize);
        return {
          content,
          number: pageNumber,
          size: this.pageSize,
          totalElements: filtered.length,
          totalPages: Math.ceil(filtered.length / this.pageSize),
          first: pageNumber === 0,
          last: start + this.pageSize >= filtered.length,
        };
      }),
    );
  }

  private emptyDraft(): PaymentDraft {
    const today = new Date();
    today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
    return {
      amount: null,
      paymentDate: today.toISOString().slice(0, 10),
      paymentMethod: 'CASH',
      notes: '',
    };
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 400) {
      const body = error.error as { message?: unknown; validationErrors?: unknown };
      if (typeof body?.validationErrors === 'object' && body.validationErrors !== null) {
        const details = Object.entries(body.validationErrors)
          .filter((entry): entry is [string, string] => typeof entry[1] === 'string')
          .map(([field, message]) => `${field}: ${message}`)
          .join(', ');
        if (details) return details;
      }
      if (typeof body?.message === 'string') return body.message;
      return 'Vérifiez les informations saisies.';
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires pour consulter ces informations financières.';
    }
    if (error instanceof HttpErrorResponse && error.status === 404) {
      return 'Le candidat ou le paiement demandé est introuvable.';
    }
    return 'Les informations financières n’ont pas pu être chargées. Veuillez réessayer.';
  }
}
