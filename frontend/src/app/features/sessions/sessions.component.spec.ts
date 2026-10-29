import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { CandidateService } from '../../core/candidates/candidate.service';
import { CircuitService } from '../../core/circuits/circuit.service';
import { DrivingSessionService } from '../../core/driving-sessions/driving-session.service';
import { StatementPage } from '../../core/financials/financial.models';
import { FinancialService } from '../../core/financials/financial.service';
import { InstructorService } from '../../core/instructors/instructor.service';
import { VehicleService } from '../../core/vehicles/vehicle.service';
import { SessionsComponent } from './sessions.component';

describe('SessionsComponent', () => {
  let sessions: jasmine.SpyObj<DrivingSessionService>;
  let candidates: jasmine.SpyObj<CandidateService>;
  let instructors: jasmine.SpyObj<InstructorService>;
  let vehicles: jasmine.SpyObj<VehicleService>;
  let circuits: jasmine.SpyObj<CircuitService>;
  let financial: jasmine.SpyObj<FinancialService>;

  beforeEach(() => {
    sessions = jasmine.createSpyObj<DrivingSessionService>('DrivingSessionService', ['findAll', 'create', 'update', 'changeStatus', 'delete']);
    candidates = jasmine.createSpyObj<CandidateService>('CandidateService', ['findAll']);
    instructors = jasmine.createSpyObj<InstructorService>('InstructorService', ['findAll']);
    vehicles = jasmine.createSpyObj<VehicleService>('VehicleService', ['findAll']);
    circuits = jasmine.createSpyObj<CircuitService>('CircuitService', ['findAll']);
    financial = jasmine.createSpyObj<FinancialService>('FinancialService', ['getStatement']);
    sessions.findAll.and.returnValue(of(emptyPage()));
    candidates.findAll.and.returnValue(of(emptyPage()));
    instructors.findAll.and.returnValue(of(emptyPage()));
    vehicles.findAll.and.returnValue(of(emptyPage()));
    circuits.findAll.and.returnValue(of(emptyPage()));
    financial.getStatement.and.returnValue(of({
      candidateId: 'candidate-id', content: [], totalSessions: 0, totalPaid: 0, remaining: 0, credit: false,
      number: 0, size: 100, totalElements: 0, totalPages: 0, first: true, last: true,
    } as StatementPage));
    sessions.create.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { message: 'Conflict with candidate schedule' },
    })));
    TestBed.configureTestingModule({
      imports: [SessionsComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { getSession: () => ({ roles: ['ADMIN'] }) } },
        { provide: DrivingSessionService, useValue: sessions },
        { provide: CandidateService, useValue: candidates },
        { provide: InstructorService, useValue: instructors },
        { provide: VehicleService, useValue: vehicles },
        { provide: CircuitService, useValue: circuits },
        { provide: FinancialService, useValue: financial },
      ],
    });
  });

  it('submits session filters to the paginated API', () => {
    const component = TestBed.createComponent(SessionsComponent).componentInstance;
    component.filters.dateFrom = '2026-10-01';
    component.filters.dateTo = '2026-10-31';
    component.filters.status = 'PLANNED';
    component.filters.candidateId = 'candidate-id';
    component.applyFilters();

    expect(sessions.findAll).toHaveBeenCalledWith(jasmine.objectContaining({
      dateFrom: '2026-10-01', dateTo: '2026-10-31', status: 'PLANNED',
      candidateId: 'candidate-id', page: 0,
    }));
  });

  it('surfaces a 409 schedule conflict when creating a session', () => {
    const component = TestBed.createComponent(SessionsComponent).componentInstance;
    component.startCreate();
    Object.assign(component.draft, {
      candidateId: 'candidate-id', instructorId: 'instructor-id',
      sessionDate: '2026-10-29', startTime: '09:00', endTime: '10:00', price: 25,
    });
    component.save();

    expect(component.errorMessage()).toContain('Conflict with candidate schedule');
  });

  it('shows each line of a 409 schedule conflict in the form', () => {
    sessions.create.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { message: 'Le moniteur est occupé.\nLe véhicule est réservé.' },
    })));
    const component = TestBed.createComponent(SessionsComponent).componentInstance;
    component.startCreate();
    Object.assign(component.draft, {
      candidateId: 'candidate-id', instructorId: 'instructor-id',
      sessionDate: '2026-10-29', startTime: '09:00', endTime: '10:00', price: 25,
    });

    component.save();

    expect(component.formConflicts()).toEqual([
      'Le moniteur est occupé.',
      'Le véhicule est réservé.',
    ]);
    expect(component.errorMessage()).toBe('Le moniteur est occupé.\nLe véhicule est réservé.');
  });

  function emptyPage() {
    return {
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 100, first: true, last: true,
    };
  }
});
