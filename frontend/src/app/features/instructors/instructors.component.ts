import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EMPTY, finalize, switchMap, take } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  CreateInstructorRequest,
  Instructor,
  InstructorStatus,
  UpdateInstructorRequest,
} from '../../core/instructors/instructor.models';
import { InstructorService } from '../../core/instructors/instructor.service';
import { LocalDatePipe } from '../../shared/pipes/local-date.pipe';
import { SearchBarComponent } from '../../shared/components/search-bar.component';
import { ConfirmationDialogService } from '../../shared/components/confirm-dialog.component';
import { applyValidationErrors } from '../../shared/forms/apply-validation-errors';

@Component({
  selector: 'app-instructors',
  standalone: true,
  imports: [ReactiveFormsModule, LocalDatePipe, SearchBarComponent, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
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
  readonly form: FormGroup;
  readonly pageNumber = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;

  constructor(
    formBuilder: FormBuilder,
    private readonly instructorService: InstructorService,
    private readonly confirmations: ConfirmationDialogService,
  ) {
    this.form = formBuilder.nonNullable.group({
      firstName: ['', [Validators.required, Validators.maxLength(120)]],
      lastName: ['', [Validators.required, Validators.maxLength(120)]],
      phone: ['', [Validators.required, Validators.maxLength(30)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
      licenseNumber: ['', [Validators.required, Validators.maxLength(100)]],
      status: ['ACTIVE' as InstructorStatus, Validators.required],
      initialPassword: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
    });
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

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.loadInstructors(0);
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
    this.form.reset({
      firstName: '', lastName: '', phone: '', email: '', licenseNumber: '',
      status: 'ACTIVE', initialPassword: '',
    });
    this.form.controls['initialPassword'].setValidators([
      Validators.required, Validators.minLength(12), Validators.maxLength(72),
    ]);
    this.form.controls['initialPassword'].updateValueAndValidity();
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  startEdit(instructor: Instructor): void {
    this.selectedInstructor.set(instructor);
    this.form.reset({
      firstName: instructor.firstName,
      lastName: instructor.lastName,
      phone: instructor.phone,
      email: instructor.email,
      licenseNumber: instructor.licenseNumber,
      status: instructor.status,
      initialPassword: '',
    });
    this.form.controls['initialPassword'].clearValidators();
    this.form.controls['initialPassword'].updateValueAndValidity();
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  cancelEdit(): void {
    this.formOpen.set(false);
    this.selectedInstructor.set(null);
    this.form.reset({
      firstName: '', lastName: '', phone: '', email: '', licenseNumber: '',
      status: 'ACTIVE', initialPassword: '',
    });
  }

  save(): void {
    if (this.saving()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values = this.form.getRawValue();
    const requestFields = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      phone: values.phone.trim(),
      email: values.email.trim(),
      licenseNumber: values.licenseNumber.trim(),
    };
    const selected = this.selectedInstructor();
    const initialPassword = values.initialPassword;

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
          initialPassword,
        } satisfies CreateInstructorRequest);
    this.form.controls['initialPassword'].reset('');

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
        error: (error: unknown) => {
          if (error instanceof HttpErrorResponse && applyValidationErrors(this.form, error.error?.validationErrors)) {
            this.errorMessage.set('Corrigez les champs indiqués.');
          } else {
            this.errorMessage.set(this.getErrorMessage(error));
          }
        },
      });
  }

  toggleStatus(instructor: Instructor): void {
    if (this.saving() || this.deletingId()) return;
    this.saving.set(true);
    this.errorMessage.set('');
    this.instructorService.update(instructor.id, {
      firstName: instructor.firstName,
      lastName: instructor.lastName,
      phone: instructor.phone,
      email: instructor.email,
      licenseNumber: instructor.licenseNumber,
      status: instructor.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
    }).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (updated) => {
        this.successMessage.set(`${updated.firstName} ${updated.lastName} ${updated.status === 'ACTIVE' ? 'activé' : 'désactivé'}.`);
        this.loadInstructors(this.pageNumber(), false);
      },
      error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
    });
  }

  deleteInstructor(instructor: Instructor): void {
    if (this.deletingId()) return;
    this.confirmations.confirm({
      title: 'Supprimer le moniteur ?',
      message: `Le profil de ${instructor.firstName} ${instructor.lastName} et son compte seront supprimés.`,
    }).pipe(
      take(1),
      switchMap((confirmed) => {
        if (!confirmed) return EMPTY;
        this.errorMessage.set('');
        this.successMessage.set('');
        this.deletingId.set(instructor.id);
        return this.instructorService.delete(instructor.id).pipe(finalize(() => this.deletingId.set(null)));
      }),
    )
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
