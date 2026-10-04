import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { AuthSession } from '../../core/auth/auth.models';
import {
  Candidate,
  CandidateStatus,
  UpdateCandidateRequest,
  UserRole,
} from '../../core/candidates/candidate.models';
import { CandidateService } from '../../core/candidates/candidate.service';

interface CandidateDraft {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  dateOfBirth: string;
  address: string;
  registrationDate: string;
  status: CandidateStatus;
  notes: string;
  userId: string | null;
}

function isUserRole(role: string): role is UserRole {
  return role === 'ADMIN' || role === 'INSTRUCTOR' || role === 'CANDIDATE';
}

@Component({
  selector: 'app-candidates',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
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
  readonly candidateDraft: CandidateDraft = {
    firstName: '',
    lastName: '',
    phone: '',
    email: '',
    dateOfBirth: '',
    address: '',
    registrationDate: '',
    status: 'ACTIVE',
    notes: '',
    userId: null,
  };
  readonly pageNumber = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;
  readonly session: AuthSession | null;
  readonly isAdmin: boolean;
  readonly availableRoles: UserRole[] = ['ADMIN', 'INSTRUCTOR', 'CANDIDATE'];
  readonly selectedRoles = signal<UserRole[]>([]);
  readonly filteredCandidates = computed(() => this.candidates());

  constructor(
    private readonly candidateService: CandidateService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
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

  onSearch(event: Event): void {
    if (event.target instanceof HTMLInputElement) {
      this.searchTerm.set(event.target.value);
      this.loadCandidates(0);
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
    this.errorMessage.set('');
    this.successMessage.set('');
    Object.assign(this.candidateDraft, {
      firstName: candidate.firstName,
      lastName: candidate.lastName,
      phone: candidate.phone,
      email: candidate.email,
      dateOfBirth: candidate.dateOfBirth ?? '',
      address: candidate.address ?? '',
      registrationDate: candidate.registrationDate,
      status: candidate.status,
      notes: candidate.notes ?? '',
      userId: candidate.userId,
    });
  }

  cancelCandidateEdit(): void {
    this.editingCandidate.set(null);
  }

  saveCandidate(): void {
    const candidate = this.editingCandidate();
    if (!this.isAdmin || !candidate || this.saving()) {
      return;
    }

    const changes: UpdateCandidateRequest = {
      ...this.candidateDraft,
      dateOfBirth: this.candidateDraft.dateOfBirth || null,
      address: this.candidateDraft.address || null,
      notes: this.candidateDraft.notes || null,
    };
    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    this.candidateService.update(candidate.id, changes)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => {
          this.candidates.update((items) => items.map((item) => item.id === updated.id ? updated : item));
          this.editingCandidate.set(updated);
          this.successMessage.set(`Candidat ${updated.firstName} ${updated.lastName} mis à jour.`);
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

    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires pour consulter les candidats.';
    }

    return 'Les candidats n’ont pas pu être chargés. Veuillez réessayer.';
  }
}
