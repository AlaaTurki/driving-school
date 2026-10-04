import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  Candidate,
  CandidatePage,
  UpdateCandidateRequest,
  UpdateCandidateRolesRequest,
} from './candidate.models';
import { CandidateService } from './candidate.service';

describe('CandidateService', () => {
  let service: CandidateService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CandidateService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads a server-filtered candidate page', () => {
    const candidate: Candidate = {
      id: 'candidate-id',
      userId: 'user-id',
      firstName: 'Candidate',
      lastName: 'Name',
      email: 'candidate@example.com',
      phone: '+216 12 345 678',
      dateOfBirth: null,
      address: null,
      registrationDate: '2026-10-03',
      status: 'ACTIVE',
      notes: null,
      createdAt: '2026-10-03T11:00:00Z',
      updatedAt: '2026-10-03T11:00:00Z',
      roles: ['CANDIDATE'],
    };
    const page: CandidatePage = {
      content: [candidate],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
      first: true,
      last: true,
    };

    service.findAll({ search: 'candidate', status: 'ACTIVE', page: 0, size: 20 })
      .subscribe((result) => expect(result).toEqual(page));
    const request = httpTesting.expectOne((req) => req.url === '/api/candidates');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('candidate');
    expect(request.request.params.get('status')).toBe('ACTIVE');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    request.flush(page);
  });

  it('updates linked account roles', () => {
    const changes: UpdateCandidateRolesRequest = { roles: ['INSTRUCTOR', 'CANDIDATE'] };

    service.updateRoles('candidate-id', changes).subscribe((result) => {
      expect(result.roles).toEqual(changes.roles);
    });
    const request = httpTesting.expectOne('/api/candidates/candidate-id/roles');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(changes);
    request.flush({
      id: 'candidate-id',
      userId: 'user-id',
      firstName: 'Candidate',
      lastName: 'Name',
      email: 'candidate@example.com',
      phone: '+216 12 345 678',
      dateOfBirth: null,
      address: null,
      registrationDate: '2026-10-03',
      status: 'ACTIVE',
      notes: null,
      createdAt: '2026-10-03T11:00:00Z',
      updatedAt: '2026-10-03T11:00:00Z',
      roles: changes.roles,
    });
  });

  it('updates candidate profile information', () => {
    const changes: UpdateCandidateRequest = {
      firstName: 'Updated',
      lastName: 'Name',
      phone: '+216 11 222 333',
      email: 'updated@example.com',
      dateOfBirth: '2000-01-02',
      address: 'New address',
      registrationDate: '2026-10-03',
      status: 'ACTIVE',
      notes: 'Updated notes',
      userId: 'user-id',
    };

    service.update('candidate-id', changes).subscribe((result) => {
      expect(result.firstName).toBe('Updated');
    });
    const request = httpTesting.expectOne('/api/candidates/candidate-id');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(changes);
    request.flush({
      id: 'candidate-id',
      ...changes,
      createdAt: '2026-10-03T11:00:00Z',
      updatedAt: '2026-10-03T11:00:00Z',
      roles: ['CANDIDATE'],
    });
  });
});
