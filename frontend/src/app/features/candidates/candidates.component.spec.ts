import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { CandidateService } from '../../core/candidates/candidate.service';
import { ConfirmationDialogService } from '../../shared/components/confirm-dialog.component';
import { CandidatesComponent } from './candidates.component';

describe('CandidatesComponent form', () => {
  let candidates: jasmine.SpyObj<CandidateService>;
  let roles: string[];

  beforeEach(() => {
    roles = ['ADMIN'];
    candidates = jasmine.createSpyObj<CandidateService>('CandidateService', [
      'findAll', 'create', 'update', 'delete', 'updateRoles',
    ]);
    candidates.findAll.and.returnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true,
    }));
    candidates.create.and.returnValue(of({
      id: 'candidate-id',
      userId: null,
      firstName: 'Ada',
      lastName: 'Lovelace',
      phone: '+216 12 345 678',
      email: 'ada@example.com',
      dateOfBirth: null,
      address: null,
      registrationDate: '2026-10-29',
      status: 'ACTIVE',
      notes: null,
      createdAt: '',
      updatedAt: '',
      roles: [],
    }));
    TestBed.configureTestingModule({
      imports: [CandidatesComponent],
      providers: [
        provideRouter([]),
        { provide: CandidateService, useValue: candidates },
        { provide: AuthService, useValue: { getSession: () => ({ roles, userId: 'admin', email: 'admin@example.com' }), logout: () => undefined } },
        { provide: ConfirmationDialogService, useValue: { confirm: () => of(false) } },
      ],
    });
  });

  it('rejects invalid required and email fields', () => {
    const component = TestBed.createComponent(CandidatesComponent).componentInstance;
    component.startCreate();
    component.saveCandidate();

    expect(component.form.invalid).toBeTrue();
    expect(candidates.create).not.toHaveBeenCalled();
  });

  it('submits a valid new candidate and clears optional empty values', () => {
    const component = TestBed.createComponent(CandidatesComponent).componentInstance;
    component.startCreate();
    component.form.patchValue({
      firstName: 'Ada',
      lastName: 'Lovelace',
      phone: '+216 12 345 678',
      email: 'ada@example.com',
      registrationDate: '2026-10-29',
      status: 'ACTIVE',
    });
    component.saveCandidate();

    expect(candidates.create).toHaveBeenCalledWith(jasmine.objectContaining({
      firstName: 'Ada',
      email: 'ada@example.com',
      dateOfBirth: null,
      address: null,
      userId: null,
    }));
  });

  it('hides candidate creation from instructors', () => {
    roles = ['INSTRUCTOR'];
    const fixture = TestBed.createComponent(CandidatesComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Nouveau candidat');
  });
});
