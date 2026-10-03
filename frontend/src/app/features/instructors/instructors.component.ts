import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import {
  CreateInstructorRequest,
  Instructor,
  InstructorStatus,
  UpdateInstructorRequest,
} from '../../core/instructors/instructor.models';
import { InstructorService } from '../../core/instructors/instructor.service';

interface InstructorForm {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  licenseNumber: string;
  status: InstructorStatus;
  initialPassword: string;
}

type InstructorTextField = Exclude<keyof InstructorForm, 'status'>;

function emptyForm(): InstructorForm {
  return {
    firstName: '',
    lastName: '',
    phone: '',
    email: '',
    licenseNumber: '',
    status: 'ACTIVE',
    initialPassword: '',
  };
}

@Component({
  selector: 'app-instructors',
  standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './instructors.component.html',
  styleUrl: './instructors.component.scss',
})
export class InstructorsComponent {
  readonly instructors = signal<Instructor[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly searchTerm = signal('');
  readonly statusFilter = signal<InstructorStatus | 'ALL'>('ALL');
  readonly selectedInstructor = signal<Instructor | null>(null);
  readonly formOpen = signal(false);
  readonly form = signal<InstructorForm>(emptyForm());
  readonly pageNumber = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;

  constructor(
    private readonly instructorService: InstructorService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.loadInstructors();
  }

  statusLabel(status: InstructorStatus): string {
    return status === 'ACTIVE' ? 'Actif' : 'Inactif';
  }

  loadInstructors(page = this.pageNumber(), clearMessages = true): void {
    if (clearMessages) {
      this.errorMessage.set('');
      this.successMessage.set('');
    }
    this.loading.set(true);
    this.instructorService.findAll({
      search: this.searchTerm(),
      status: this.statusFilter(),
      page,
      size: this.pageSize,
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.instructors.set(result.content);
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
      this.loadInstructors(0);
    }
  }

  onStatusChange(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      const value = event.target.value;
      this.statusFilter.set(value === 'ACTIVE' || value === 'INACTIVE' ? value : 'ALL');
      this.loadInstructors(0);
    }
  }

  previousPage(): void {
    if (this.pageNumber() > 0 && !this.loading()) {
      this.loadInstructors(this.pageNumber() - 1);
    }
  }

  nextPage(): void {
    if (this.pageNumber() + 1 < this.totalPages() && !this.loading()) {
      this.loadInstructors(this.pageNumber() + 1);
    }
  }

  startCreate(): void {
    this.selectedInstructor.set(null);
    this.form.set(emptyForm());
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  startEdit(instructor: Instructor): void {
    this.selectedInstructor.set(instructor);
    this.form.set({
      firstName: instructor.firstName,
      lastName: instructor.lastName,
      phone: instructor.phone,
      email: instructor.email,
      licenseNumber: instructor.licenseNumber,
      status: instructor.status,
      initialPassword: '',
    });
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  setTextField(field: InstructorTextField, event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLInputElement)) {
      return;
    }
    this.form.update((current) => ({ ...current, [field]: target.value }));
  }

  setStatus(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      const value = event.target.value;
      if (value === 'ACTIVE' || value === 'INACTIVE') {
        this.form.update((current) => ({ ...current, status: value }));
      }
    }
  }

  cancelEdit(): void {
    this.formOpen.set(false);
    this.selectedInstructor.set(null);
    this.form.set(emptyForm());
  }

  save(event: Event): void {
    event.preventDefault();
    if (this.saving()) {
      return;
    }
    const values = this.form();
    const requestFields = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      phone: values.phone.trim(),
      email: values.email.trim(),
      licenseNumber: values.licenseNumber.trim(),
    };
    const selected = this.selectedInstructor();

    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    const request = selected
      ? this.instructorService.update(selected.id, {
          ...requestFields,
          status: values.status,
        } satisfies UpdateInstructorRequest)
      : this.instructorService.create({
          ...requestFields,
          initialPassword: values.initialPassword,
        } satisfies CreateInstructorRequest);

    request
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (saved) => {
          this.cancelEdit();
          this.successMessage.set(
            selected
              ? `Les coordonnées de ${saved.firstName} ${saved.lastName} ont été mises à jour.`
              : `Le compte de ${saved.firstName} ${saved.lastName} a été créé.`,
          );
          this.loadInstructors(0, false);
        },
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  deleteInstructor(instructor: Instructor): void {
    if (this.deletingId() || !window.confirm(`Supprimer ${instructor.firstName} ${instructor.lastName} et son compte ?`)) {
      return;
    }
    this.errorMessage.set('');
    this.successMessage.set('');
    this.deletingId.set(instructor.id);
    this.instructorService.delete(instructor.id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => {
          if (this.selectedInstructor()?.id === instructor.id) {
            this.cancelEdit();
          }
          this.successMessage.set(
            `${instructor.firstName} ${instructor.lastName} et son compte ont été supprimés.`,
          );
          this.loadInstructors(0, false);
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
      return 'Seuls les administrateurs peuvent gérer les moniteurs.';
    }
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Cet e-mail ou ce numéro de permis est déjà utilisé.';
    }
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return 'Vérifiez les champs saisis et le mot de passe initial (12 caractères minimum).';
    }
    return 'L’opération n’a pas abouti. Veuillez réessayer.';
  }
}
