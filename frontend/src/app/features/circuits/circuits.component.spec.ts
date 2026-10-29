import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { CircuitService } from '../../core/circuits/circuit.service';
import { ConfirmationDialogService } from '../../shared/components/confirm-dialog.component';
import { CircuitsComponent } from './circuits.component';

describe('CircuitsComponent form', () => {
  let circuits: jasmine.SpyObj<CircuitService>;
  let roles: string[];

  beforeEach(() => {
    roles = ['ADMIN'];
    circuits = jasmine.createSpyObj<CircuitService>('CircuitService', ['findAll', 'create', 'update', 'delete']);
    circuits.findAll.and.returnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true,
    }));
    circuits.create.and.returnValue(of({
      id: 'circuit-id', name: 'Centre', description: null, location: null,
      price: 12.5, active: true, createdAt: '', updatedAt: '',
    }));
    TestBed.configureTestingModule({
      imports: [CircuitsComponent],
      providers: [
        provideRouter([]),
        { provide: CircuitService, useValue: circuits },
        { provide: AuthService, useValue: { getSession: () => ({ roles }), logout: () => undefined } },
        { provide: ConfirmationDialogService, useValue: { confirm: () => of(false) } },
      ],
    });
  });

  it('requires a valid name and non-negative price with at most three decimals', () => {
    const component = TestBed.createComponent(CircuitsComponent).componentInstance;
    component.startCreate();
    component.form.patchValue({ price: '1.2345' });
    component.save();

    expect(component.form.invalid).toBeTrue();
    expect(circuits.create).not.toHaveBeenCalled();
  });

  it('submits a valid circuit with numeric price and null optional fields', () => {
    const component = TestBed.createComponent(CircuitsComponent).componentInstance;
    component.startCreate();
    component.form.patchValue({ name: ' Centre ', price: '12.500' });
    component.save();

    expect(circuits.create).toHaveBeenCalledWith({
      name: 'Centre', description: null, location: null, price: 12.5, active: true,
    });
  });

  it('hides circuit creation from instructors', () => {
    roles = ['INSTRUCTOR'];
    const fixture = TestBed.createComponent(CircuitsComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Nouveau circuit');
  });
});
