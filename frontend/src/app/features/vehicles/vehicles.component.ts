import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EMPTY, finalize, switchMap, take } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { SaveVehicleRequest, Vehicle, VehicleType } from '../../core/vehicles/vehicle.models';
import { VehicleService } from '../../core/vehicles/vehicle.service';
import { SearchBarComponent } from '../../shared/components/search-bar.component';
import { ConfirmationDialogService } from '../../shared/components/confirm-dialog.component';
import { applyValidationErrors } from '../../shared/forms/apply-validation-errors';

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [ReactiveFormsModule, SearchBarComponent, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './vehicles.component.html',
  styleUrl: './vehicles.component.scss',
})
export class VehiclesComponent {
  readonly vehicles = signal<Vehicle[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly searchTerm = signal('');
  readonly activeFilter = signal<boolean | 'ALL'>('ALL');
  readonly formOpen = signal(false);
  readonly selectedVehicle = signal<Vehicle | null>(null);
  readonly form: FormGroup;
  readonly pageNumber = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;
  readonly isAdmin: boolean;
  readonly vehicleTypes: VehicleType[] = ['MANUAL', 'AUTOMATIC', 'MOTORCYCLE', 'TRUCK'];

  constructor(
    formBuilder: FormBuilder,
    private readonly vehicleService: VehicleService,
    private readonly authService: AuthService,
    private readonly confirmations: ConfirmationDialogService,
  ) {
    this.form = formBuilder.nonNullable.group({
      registrationNumber: ['', [Validators.required, Validators.maxLength(30)]],
      brand: ['', [Validators.required, Validators.maxLength(120)]],
      model: ['', [Validators.required, Validators.maxLength(120)]],
      type: ['MANUAL' as VehicleType, Validators.required],
      active: [true, Validators.required],
    });
    this.isAdmin = this.authService.getSession()?.roles.includes('ADMIN') ?? false;
    this.loadVehicles();
  }

  loadVehicles(page = this.pageNumber()): void {
    this.errorMessage.set('');
    this.loading.set(true);
    this.vehicleService.findAll({
      search: this.searchTerm(),
      active: this.activeFilter(),
      page,
      size: this.pageSize,
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.vehicles.set(result.content);
          this.pageNumber.set(result.number);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
        },
        error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
      });
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.loadVehicles(0);
  }

  onActiveChange(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      this.activeFilter.set(event.target.value === 'true' ? true : event.target.value === 'false' ? false : 'ALL');
      this.loadVehicles(0);
    }
  }

  startCreate(): void {
    this.selectedVehicle.set(null);
    this.form.reset({ registrationNumber: '', brand: '', model: '', type: 'MANUAL', active: true });
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  startEdit(vehicle: Vehicle): void {
    this.selectedVehicle.set(vehicle);
    this.form.reset({
      registrationNumber: vehicle.registrationNumber,
      brand: vehicle.brand,
      model: vehicle.model,
      type: vehicle.type,
      active: vehicle.active,
    });
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  cancelCreate(): void {
    this.formOpen.set(false);
    this.selectedVehicle.set(null);
    this.form.reset({ registrationNumber: '', brand: '', model: '', type: 'MANUAL', active: true });
  }

  save(): void {
    if (!this.isAdmin || this.saving()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values = this.form.getRawValue();
    const selected = this.selectedVehicle();
    const request: SaveVehicleRequest = {
      registrationNumber: values.registrationNumber.trim().toUpperCase(),
      brand: values.brand.trim(),
      model: values.model.trim(),
      type: values.type,
      active: values.active,
    };
    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    const operation = selected
      ? this.vehicleService.update(selected.id, request)
      : this.vehicleService.create(request);
    operation
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (vehicle) => {
          this.cancelCreate();
          this.successMessage.set(selected
            ? `Le véhicule ${vehicle.registrationNumber} a été modifié.`
            : `Le véhicule ${vehicle.registrationNumber} a été créé.`);
          this.loadVehicles(0);
        },
        error: (error: unknown) => {
          if (error instanceof HttpErrorResponse && applyValidationErrors(this.form, error.error?.validationErrors)) {
            this.errorMessage.set('Corrigez les champs indiqués.');
          } else {
            this.errorMessage.set(this.errorText(error));
          }
        },
      });
  }

  toggleActive(vehicle: Vehicle): void {
    if (!this.isAdmin || this.saving()) return;
    this.saving.set(true);
    this.vehicleService.update(vehicle.id, {
      registrationNumber: vehicle.registrationNumber,
      brand: vehicle.brand,
      model: vehicle.model,
      type: vehicle.type,
      active: !vehicle.active,
    }).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (updated) => {
        this.successMessage.set(`${updated.registrationNumber} ${updated.active ? 'activé' : 'désactivé'}.`);
        this.loadVehicles(this.pageNumber());
      },
      error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
    });
  }

  deleteVehicle(vehicle: Vehicle): void {
    if (this.deletingId()) return;
    this.confirmations.confirm({
      title: 'Supprimer le véhicule ?',
      message: `Le véhicule ${vehicle.registrationNumber} sera supprimé. Cette action est irréversible.`,
    }).pipe(
      take(1),
      switchMap((confirmed) => {
        if (!confirmed) return EMPTY;
        this.deletingId.set(vehicle.id);
        this.errorMessage.set('');
        return this.vehicleService.delete(vehicle.id).pipe(finalize(() => this.deletingId.set(null)));
      }),
    ).subscribe({
      next: () => {
        this.successMessage.set(`Le véhicule ${vehicle.registrationNumber} a été supprimé.`);
        this.loadVehicles(0);
      },
      error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
    });
  }

  previousPage(): void {
    if (this.pageNumber() > 0 && !this.loading()) {
      this.loadVehicles(this.pageNumber() - 1);
    }
  }

  nextPage(): void {
    if (this.pageNumber() + 1 < this.totalPages() && !this.loading()) {
      this.loadVehicles(this.pageNumber() + 1);
    }
  }

  private errorText(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires pour consulter les véhicules.';
    }
    if (error instanceof HttpErrorResponse && error.status === 409) {
      if (String(error.error?.message ?? '').toLowerCase().includes('driving sessions')
        || String(error.error?.message ?? '').toLowerCase().includes('session')) {
        return 'Ce véhicule possède des séances associées. Désactivez-le plutôt que de le supprimer.';
      }
      return 'Cette immatriculation est déjà utilisée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return 'Vérifiez les champs saisis et le type de véhicule.';
    }
    return 'Les véhicules n’ont pas pu être chargés. Veuillez réessayer.';
  }
}
