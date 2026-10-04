import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { SaveVehicleRequest, Vehicle, VehicleType } from '../../core/vehicles/vehicle.models';
import { VehicleService } from '../../core/vehicles/vehicle.service';

interface VehicleDraft {
  registrationNumber: string;
  brand: string;
  model: string;
  type: VehicleType;
}

function emptyDraft(): VehicleDraft {
  return { registrationNumber: '', brand: '', model: '', type: 'MANUAL' };
}

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './vehicles.component.html',
  styleUrl: './vehicles.component.scss',
})
export class VehiclesComponent {
  readonly vehicles = signal<Vehicle[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly searchTerm = signal('');
  readonly activeFilter = signal<boolean | 'ALL'>('ALL');
  readonly formOpen = signal(false);
  readonly draft = signal<VehicleDraft>(emptyDraft());
  readonly pageNumber = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;
  readonly isAdmin: boolean;
  readonly vehicleTypes: VehicleType[] = ['MANUAL', 'AUTOMATIC', 'MOTORCYCLE', 'TRUCK'];

  constructor(
    private readonly vehicleService: VehicleService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
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

  onSearch(event: Event): void {
    if (event.target instanceof HTMLInputElement) {
      this.searchTerm.set(event.target.value);
      this.loadVehicles(0);
    }
  }

  onActiveChange(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      this.activeFilter.set(event.target.value === 'true' ? true : event.target.value === 'false' ? false : 'ALL');
      this.loadVehicles(0);
    }
  }

  setField(field: keyof VehicleDraft, event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLInputElement || target instanceof HTMLSelectElement)) {
      return;
    }
    this.draft.update((draft) => ({ ...draft, [field]: target.value }));
  }

  startCreate(): void {
    this.draft.set(emptyDraft());
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  cancelCreate(): void {
    this.formOpen.set(false);
    this.draft.set(emptyDraft());
  }

  save(event: Event): void {
    event.preventDefault();
    if (!this.isAdmin || this.saving()) {
      return;
    }
    const draft = this.draft();
    const request: SaveVehicleRequest = {
      registrationNumber: draft.registrationNumber.trim().toUpperCase(),
      brand: draft.brand.trim(),
      model: draft.model.trim(),
      type: draft.type,
      active: true,
    };
    this.errorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);
    this.vehicleService.create(request)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (vehicle) => {
          this.cancelCreate();
          this.successMessage.set(`Le véhicule ${vehicle.registrationNumber} a été créé.`);
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

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private errorText(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires pour consulter les véhicules.';
    }
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Cette immatriculation est déjà utilisée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return 'Vérifiez les champs saisis et le type de véhicule.';
    }
    return 'Les véhicules n’ont pas pu être chargés. Veuillez réessayer.';
  }
}
