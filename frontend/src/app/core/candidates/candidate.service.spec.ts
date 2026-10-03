import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
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

  it('loads the candidate list', () => {
    const candidates = [{
      id: 'candidate-id',
      fullName: 'Candidate Name',
      email: 'candidate@example.com',
      phone: '+216 12 345 678',
      status: 'ACTIVE' as const,
      registeredAt: '2026-10-03T11:00:00Z',
    }];

    service.findAll().subscribe((result) => expect(result).toEqual(candidates));
    const request = httpTesting.expectOne('/api/candidates');
    expect(request.request.method).toBe('GET');
    request.flush(candidates);
  });

  it('updates the candidate profile and status', () => {
    const changes = { fullName: 'Updated Name', phone: '+216 98 765 432', status: 'INACTIVE' as const };

    service.update('candidate-id', changes).subscribe((result) => {
      expect(result.fullName).toBe('Updated Name');
      expect(result.status).toBe('INACTIVE');
    });
    const request = httpTesting.expectOne('/api/candidates/candidate-id');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(changes);
    request.flush({
      id: 'candidate-id',
      ...changes,
      email: 'candidate@example.com',
      registeredAt: '2026-10-03T11:00:00Z',
    });
  });
});
