import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { VehicleService } from '../../core/vehicles/vehicle.service';
import { ConfirmationDialogService } from '../../shared/components/confirm-dialog.component';
import { VehiclesComponent } from './vehicles.component';

describe('VehiclesComponent form', () => {
  let vehicles: jasmine.SpyObj<VehicleService>;
  let roles: string[];

  beforeEach(() => {
    roles = ['ADMIN'];
    vehicles = jasmine.createSpyObj<VehicleService>('VehicleService', ['findAll', 'create', 'update', 'delete']);
    vehicles.findAll.and.returnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true,
    }));
    vehicles.create.and.returnValue(of({
      id: 'vehicle-id', registrationNumber: 'ABC-123', brand: 'Toyota',
      model: 'Yaris', type: 'MANUAL', active: true, createdAt: '', updatedAt: '',
    }));
    TestBed.configureTestingModule({
      imports: [VehiclesComponent],
      providers: [
        { provide: VehicleService, useValue: vehicles },
        { provide: AuthService, useValue: { getSession: () => ({ roles }), logout: () => undefined } },
        { provide: ConfirmationDialogService, useValue: { confirm: () => of(false) } },
      ],
    });
  });

  it('validates required vehicle fields before submission', () => {
    const component = TestBed.createComponent(VehiclesComponent).componentInstance;
    component.startCreate();
    component.save();

    expect(component.form.invalid).toBeTrue();
    expect(vehicles.create).not.toHaveBeenCalled();
  });

  it('submits a valid vehicle with normalized registration', () => {
    const component = TestBed.createComponent(VehiclesComponent).componentInstance;
    component.startCreate();
    component.form.patchValue({ registrationNumber: ' abc-123 ', brand: 'Toyota', model: 'Yaris' });
    component.save();

    expect(vehicles.create).toHaveBeenCalledWith(jasmine.objectContaining({
      registrationNumber: 'ABC-123', brand: 'Toyota', model: 'Yaris', active: true,
    }));
  });

  it('hides vehicle creation from instructors', () => {
    roles = ['INSTRUCTOR'];
    const fixture = TestBed.createComponent(VehiclesComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Nouveau véhicule');
  });
});
