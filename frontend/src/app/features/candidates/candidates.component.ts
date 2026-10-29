import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, finalize, switchMap, take } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { AuthSession } from '../../core/auth/auth.models';
import {
  Candidate,
  CandidateStatus,
  SaveCandidateRequest,
  UpdateCandidateRequest,
  UserRole,
} from '../../core/candidates/candidate.models';
import { CandidateService } from '../../core/candidates/candidate.service';
import { SearchBarComponent } from '../../shared/components/search-bar.component';
import { LocalDatePipe } from '../../shared/pipes/local-date.pipe';
import { ConfirmationDialogService } from '../../shared/components/confirm-dialog.component';
import { applyValidationErrors } from '../../shared/forms/apply-validation-errors';

function isUserRole(role: string): role is UserRole {
  return role === 'ADMIN' || role === 'INSTRUCTOR' || role === 'CANDIDATE';
}

@Component({
  selector: 'app-candidates',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, SearchBarComponent, LocalDatePipe],
  templateUrl: './candidates.component.html',
  styleUrl: './candidates.component.scss',
})
export class CandidatesComponent {
  readonly candidates = signal<Candidate[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly searchTerm = signal('');
  readonly statusFilter = signal<CandidateStatus | 'ALL'>('ALL');
  readonly selectedCandidate = signal<Candidate | null>(null);
  readonly editingCandidate = signal<Candidate | null>(null);
  readonly candidateFormOpen = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly form: FormGroup;
  readonly pageNumber = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;
  readonly session: AuthSession | null;
  readonly isAdmin: boolean;
  readonly availableRoles: UserRole[] = ['ADMIN', 'INSTRUCTOR', 'CANDIDATE'];
  readonly selectedRoles = signal<UserRole[]>([]);
  readonly filteredCandidates = computed(() => this.candidates());
  readonly sortKey = signal<keyof Candidate>('lastName');
  readonly sortAscending = signal(true);
  readonly sortedCandidates = computed(() => {
    const key = this.sortKey();
    const direction = this.sortAscending() ? 1 : -1;
    return [...this.filteredCandidates()].sort((left, right) =>
      String(left[key] ?? '').localeCompare(String(right[key] ?? ''), undefined, { numeric: true }) * direction);
  });

  constructor(
    formBuilder: FormBuilder,
    private readonly candidateService: CandidateService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly confirmations: ConfirmationDialogService,
  ) {
    this.form = formBuilder.nonNullable.group({
      firstName: ['', [Validators.required, Validators.maxLength(120)]],
      lastName: ['', [Validators.required, Validators.maxLength(120)]],
      phone: ['', [Validators.required, Validators.maxLength(30)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
      dateOfBirth: [''],
      address: ['', Validators.maxLength(500)],
      registrationDate: [new Date().toISOString().slice(0, 10), Validators.required],
      status: ['ACTIVE' as CandidateStatus, Validators.required],
      notes: ['', Validators.maxLength(10000)],
    });
    this.session = this.authService.getSession();
    this.isAdmin = this.session?.roles.includes('ADMIN') ?? false;
    this.loadCandidates();
  }

  roleLabel(candidate: Candidate): string {
    const roles = this.rolesFor(candidate);
    return roles.length ? roles.join(' · ') : '—';
  }

  statusLabel(status: CandidateStatus): string {
    switch (status) {
      case 'ACTIVE': return 'Actif';
      case 'INACTIVE': return 'Inactif';
      case 'COMPLETED': return 'Terminé';
      case 'SUSPENDED': return 'Suspendu';
    }
  }

  loadCandidates(page = this.pageNumber()): void {
    this.errorMessage.set('');
    this.loading.set(true);
    this.candidateService.findAll({
      search: this.searchTerm(),
      status: this.statusFilter(),
      page,
      size: this.pageSize,
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.candidates.set(result.content);
          this.pageNumber.set(result.number);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
        },
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.loadCandidates(0);
  }

  sortBy(key: keyof Candidate): void {
    if (this.sortKey() === key) this.sortAscending.update((ascending) => !ascending);
    else {
      this.sortKey.set(key);
      this.sortAscending.set(true);
    }
  }

  onStatusChange(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      const value = event.target.value;
      this.statusFilter.set(
        value === 'ACTIVE' || value === 'INACTIVE' || value === 'COMPLETED' || value === 'SUSPENDED'
          ? value
          : 'ALL',
      );
      this.loadCandidates(0);
    }
  }

  previousPage(): void {
    if (this.pageNumber() > 0 && !this.loading()) {
      this.loadCandidates(this.pageNumber() - 1);
    }
  }

  nextPage(): void {
    if (this.pageNumber() + 1 < this.totalPages() && !this.loading()) {
      this.loadCandidates(this.pageNumber() + 1);
    }
  }

  editRoles(candidate: Candidate): void {
    this.selectedCandidate.set(candidate);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.selectedRoles.set(this.rolesFor(candidate));
  }

  editCandidate(candidate: Candidate): void {
    this.editingCandidate.set(candidate);
    this.candidateFormOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.form.reset({
      firstName: candidate.firstName,
      lastName: candidate.lastName,
      phone: candidate.phone,
      email: candidate.email,
      dateOfBirth: candidate.dateOfBirth ?? '',
      address: candidate.address ?? '',
      registrationDate: candidate.registrationDate,
      status: candidate.status,
      notes: candidate.notes ?? '',
    });
  }

  startCreate(): void {
    this.editingCandidate.set(null);
    this.candidateFormOpen.set(true);
    this.form.reset({
      firstName: '',
      lastName: '',
      phone: '',
      email: '',
      dateOfBirth: '',
      address: '',
      registrationDate: new Date().toISOString().slice(0, 10),
      status: 'ACTIVE',
      notes: '',
    });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  openDetails(candidate: Candidate): void {
    void this.router.navigate(['/candidates', candidate.id]);
  }

  openProgress(candidate: Candidate): void {
    void this.router.navigate(['/candidates', candidate.id, 'skills']);
  }

  cancelCandidateEdit(): void {
    this.candidateFormOpen.set(false);
    this.editingCandidate.set(null);
    this.form.reset();
  }

  saveCandidate(): void {
    if (!this.isAdmin || this.saving()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const candidate = this.editingCandidate();
    const values = this.form.getRawValue();
    const changes: SaveCandidateRequest = {
      ...values,
      dateOfBirth: values.dateOfBirth || null,
      address: values.address.trim() || null,
      notes: values.notes.trim() || null,
      userId: candidate?.userId ?? null,
    };
    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    const request = candidate
      ? this.candidateService.update(candidate.id, changes satisfies UpdateCandidateRequest)
      : this.candidateService.create(changes);
    request
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => {
          this.cancelCandidateEdit();
          this.successMessage.set(candidate
            ? `Candidat ${updated.firstName} ${updated.lastName} mis à jour.`
            : `Candidat ${updated.firstName} ${updated.lastName} créé.`);
          this.loadCandidates(0);
        },
        error: (error: unknown) => {
          if (error instanceof HttpErrorResponse && applyValidationErrors(this.form, error.error?.validationErrors)) {
            this.errorMessage.set('Corrigez les champs indiqués.');
          } else {
            this.errorMessage.set(this.getErrorMessage(error));
          }
        },
      });
  }

  deleteCandidate(candidate: Candidate): void {
    if (this.deletingId()) return;
    this.confirmations.confirm({
      title: 'Supprimer le candidat ?',
      message: `Le profil de ${candidate.firstName} ${candidate.lastName} sera supprimé. Cette action est irréversible.`,
    }).pipe(
      take(1),
      switchMap((confirmed) => {
        if (!confirmed) return EMPTY;
        this.deletingId.set(candidate.id);
        this.errorMessage.set('');
        return this.candidateService.delete(candidate.id).pipe(finalize(() => this.deletingId.set(null)));
      }),
    ).subscribe({
      next: () => {
        this.successMessage.set(`Candidat ${candidate.firstName} ${candidate.lastName} supprimé.`);
        this.loadCandidates(0);
      },
      error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
    });
  }

  cancelEdit(): void {
    this.selectedCandidate.set(null);
    this.selectedRoles.set([]);
  }

  onRoleToggle(role: UserRole, event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLInputElement)) {
      return;
    }
    this.selectedRoles.update((roles) => target.checked
      ? [...roles, role]
      : roles.filter((selectedRole) => selectedRole !== role));
  }

  saveRoles(): void {
    const candidate = this.selectedCandidate();
    const roles = this.selectedRoles();
    if (!this.isAdmin || !candidate?.userId || roles.length === 0 || this.saving()) {
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    this.candidateService.updateRoles(candidate.id, { roles })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => {
          this.candidates.update((items) => items.map((item) => item.id === updated.id ? updated : item));
          this.selectedCandidate.set(updated);
          this.successMessage.set(`Rôles de ${updated.firstName} ${updated.lastName} mis à jour.`);
        },
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private rolesFor(candidate: Candidate): UserRole[] {
    const roles = candidate.roles?.filter(isUserRole);
    if (roles?.length) {
      return roles;
    }

    return candidate.userId === this.session?.userId
      ? this.session.roles.filter(isUserRole)
      : [];
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
        if (details) {
          return details;
        }
      }
      if (typeof body?.message === 'string') {
        return body.message;
      }
    }

    if (error instanceof HttpErrorResponse && error.status === 409) {
      const message = String(error.error?.message ?? '').toLowerCase();
      if (message.includes('session') || message.includes('payment')) {
        return 'Ce candidat ne peut pas être supprimé car des séances ou des paiements lui sont associés.';
      }
      return 'Cette adresse e-mail est déjà utilisée.';
    }

    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires pour consulter les candidats.';
    }

    return 'Les candidats n’ont pas pu être chargés. Veuillez réessayer.';
  }
}
