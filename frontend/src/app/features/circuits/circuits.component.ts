import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, finalize, switchMap, take } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { Circuit, SaveCircuitRequest } from '../../core/circuits/circuit.models';
import { CircuitService } from '../../core/circuits/circuit.service';
import { ConfirmationDialogService } from '../../shared/components/confirm-dialog.component';
import { SearchBarComponent } from '../../shared/components/search-bar.component';
import { DtCurrencyPipe } from '../../shared/pipes/dt-currency.pipe';
import { applyValidationErrors } from '../../shared/forms/apply-validation-errors';

@Component({
  selector: 'app-circuits',
  standalone: true,
  imports: [ReactiveFormsModule, SearchBarComponent, DtCurrencyPipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './circuits.component.html',
  styleUrl: './circuits.component.scss',
})
export class CircuitsComponent {
  readonly circuits = signal<Circuit[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly searchTerm = signal('');
  readonly activeFilter = signal<boolean | 'ALL'>('ALL');
  readonly pageNumber = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;
  readonly isAdmin: boolean;
  readonly formOpen = signal(false);
  readonly selectedCircuit = signal<Circuit | null>(null);
  readonly form: FormGroup;
  readonly sortAscending = signal(true);
  readonly sortedCircuits = computed(() => [...this.circuits()].sort((a, b) =>
    a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }) * (this.sortAscending() ? 1 : -1)));

  constructor(
    formBuilder: FormBuilder,
    private readonly circuitService: CircuitService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly confirmations: ConfirmationDialogService,
  ) {
    this.isAdmin = this.authService.getSession()?.roles.includes('ADMIN') ?? false;
    this.form = formBuilder.nonNullable.group({
      name: ['', [Validators.required, Validators.maxLength(160)]],
      description: ['', Validators.maxLength(10000)],
      location: ['', Validators.maxLength(200)],
      price: ['0.000', [Validators.required, Validators.pattern(/^\d{1,7}(?:\.\d{1,3})?$/), Validators.min(0)]],
      active: [true, Validators.required],
    });
    this.loadCircuits();
  }

  loadCircuits(page = this.pageNumber()): void {
    this.errorMessage.set('');
    this.loading.set(true);
    this.circuitService.findAll({
      search: this.searchTerm(),
      active: this.activeFilter(),
      page,
      size: this.pageSize,
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (result) => {
        this.circuits.set(result.content);
        this.pageNumber.set(result.number);
        this.totalPages.set(result.totalPages);
        this.totalElements.set(result.totalElements);
      },
      error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
    });
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.loadCircuits(0);
  }

  onActiveChange(event: Event): void {
    if (!(event.target instanceof HTMLSelectElement)) return;
    this.activeFilter.set(event.target.value === 'true' ? true : event.target.value === 'false' ? false : 'ALL');
    this.loadCircuits(0);
  }

  toggleSort(): void {
    this.sortAscending.update((ascending) => !ascending);
  }

  startCreate(): void {
    this.selectedCircuit.set(null);
    this.form.reset({ name: '', description: '', location: '', price: '0.000', active: true });
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  startEdit(circuit: Circuit): void {
    this.selectedCircuit.set(circuit);
    this.form.reset({
      name: circuit.name,
      description: circuit.description ?? '',
      location: circuit.location ?? '',
      price: circuit.price.toFixed(3),
      active: circuit.active,
    });
    this.formOpen.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  cancelEdit(): void {
    this.formOpen.set(false);
    this.selectedCircuit.set(null);
  }

  save(): void {
    if (!this.isAdmin || this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue();
    const request: SaveCircuitRequest = {
      name: values.name.trim(),
      description: values.description.trim() || null,
      location: values.location.trim() || null,
      price: Number(values.price),
      active: values.active,
    };
    const selected = this.selectedCircuit();
    this.saving.set(true);
    this.errorMessage.set('');
    const operation = selected
      ? this.circuitService.update(selected.id, request)
      : this.circuitService.create(request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (circuit) => {
        this.cancelEdit();
        this.successMessage.set(selected ? `${circuit.name} a été modifié.` : `${circuit.name} a été créé.`);
        this.loadCircuits(0);
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

  toggleActive(circuit: Circuit): void {
    if (!this.isAdmin || this.saving()) return;
    this.saving.set(true);
    this.circuitService.update(circuit.id, {
      name: circuit.name,
      description: circuit.description,
      location: circuit.location,
      price: circuit.price,
      active: !circuit.active,
    }).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (updated) => {
        this.successMessage.set(`${updated.name} ${updated.active ? 'activé' : 'désactivé'}.`);
        this.loadCircuits(this.pageNumber());
      },
      error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
    });
  }

  deleteCircuit(circuit: Circuit): void {
    if (this.deletingId()) return;
    this.confirmations.confirm({
      title: 'Supprimer le circuit ?',
      message: `Le circuit « ${circuit.name} » sera supprimé. Cette action est irréversible.`,
    }).pipe(
      take(1),
      switchMap((confirmed) => {
        if (!confirmed) return EMPTY;
        this.deletingId.set(circuit.id);
        this.errorMessage.set('');
        return this.circuitService.delete(circuit.id).pipe(finalize(() => this.deletingId.set(null)));
      }),
    ).subscribe({
      next: () => {
        this.successMessage.set(`Le circuit ${circuit.name} a été supprimé.`);
        this.loadCircuits(0);
      },
      error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
    });
  }

  previousPage(): void {
    if (!this.loading() && this.pageNumber() > 0) this.loadCircuits(this.pageNumber() - 1);
  }

  nextPage(): void {
    if (!this.loading() && this.pageNumber() + 1 < this.totalPages()) this.loadCircuits(this.pageNumber() + 1);
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private getErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) return 'L’opération a échoué. Veuillez réessayer.';
    if (error.status === 0) return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    if (error.status === 403) return 'Vous ne disposez pas des droits nécessaires pour gérer les circuits.';
    if (error.status === 409) {
      const message = String(error.error?.message ?? '').toLowerCase();
      if (message.includes('driving sessions') || message.includes('séance')) {
        return 'Ce circuit est utilisé par des séances. Désactivez-le plutôt que de le supprimer.';
      }
      return 'Un circuit porte déjà ce nom.';
    }
    if (error.status === 400) return 'Vérifiez le nom, le prix (3 décimales maximum) et les champs saisis.';
    return 'Les circuits n’ont pas pu être chargés. Veuillez réessayer.';
  }
}
