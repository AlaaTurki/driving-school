import { DatePipe } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { Candidate, CandidateStatus } from '../../core/candidates/candidate.models';
import { CandidateService } from '../../core/candidates/candidate.service';

@Component({
  selector: 'app-candidates',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
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
  readonly statusFilter = signal<'ALL' | CandidateStatus>('ALL');
  readonly selectedCandidate = signal<Candidate | null>(null);
  readonly filteredCandidates = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase();
    const status = this.statusFilter();
    return this.candidates().filter((candidate) => {
      const matchesSearch = !term || [candidate.fullName, candidate.email, candidate.phone]
        .some((value) => value.toLocaleLowerCase().includes(term));
      return matchesSearch && (status === 'ALL' || candidate.status === status);
    });
  });
  readonly form;

  constructor(
    formBuilder: FormBuilder,
    private readonly candidateService: CandidateService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.form = formBuilder.nonNullable.group({
      fullName: ['', [Validators.required, Validators.maxLength(120)]],
      phone: ['', [Validators.required, Validators.pattern(/^(?=(?:\D*\d){7})[+()0-9. -]{7,30}$/)]],
      status: ['ACTIVE' as CandidateStatus, Validators.required],
    });
    this.loadCandidates();
  }

  loadCandidates(): void {
    this.errorMessage.set('');
    this.loading.set(true);
    this.candidateService.findAll()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (candidates) => this.candidates.set(candidates),
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  edit(candidate: Candidate): void {
    this.selectedCandidate.set(candidate);
    this.successMessage.set('');
    this.errorMessage.set('');
    this.form.setValue({
      fullName: candidate.fullName,
      phone: candidate.phone,
      status: candidate.status,
    });
  }

  cancelEdit(): void {
    this.selectedCandidate.set(null);
    this.form.reset({ fullName: '', phone: '', status: 'ACTIVE' });
  }

  onSearch(event: Event): void {
    if (event.target instanceof HTMLInputElement) {
      this.searchTerm.set(event.target.value);
    }
  }

  onStatusChange(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      const value = event.target.value;
      this.statusFilter.set(value === 'ACTIVE' || value === 'INACTIVE' ? value : 'ALL');
    }
  }

  save(): void {
    const candidate = this.selectedCandidate();
    if (!candidate || this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    this.candidateService.update(candidate.id, this.form.getRawValue())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => {
          this.candidates.update((items) => items.map((item) => item.id === updated.id ? updated : item));
          this.selectedCandidate.set(null);
          this.successMessage.set(`${updated.fullName} : profil mis à jour.`);
          this.form.reset({ fullName: '', phone: '', status: 'ACTIVE' });
        },
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    }

    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires pour gérer les candidats.';
    }

    return 'Les candidats n’ont pas pu être chargés ou mis à jour. Veuillez réessayer.';
  }
}
