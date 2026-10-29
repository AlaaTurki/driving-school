import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DrivingSessionService } from './driving-session.service';

describe('DrivingSessionService', () => {
  let service: DrivingSessionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DrivingSessionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends date, candidate, instructor, status and circuit filters with pagination', () => {
    service.findAll({
      dateFrom: '2026-10-01', dateTo: '2026-10-31', candidateId: 'candidate',
      instructorId: 'instructor', status: 'DONE', circuitId: 'circuit', page: 1, size: 20,
    }).subscribe();
    const request = http.expectOne((req) => req.url === '/api/driving-sessions');
    expect(request.request.params.get('dateFrom')).toBe('2026-10-01');
    expect(request.request.params.get('dateTo')).toBe('2026-10-31');
    expect(request.request.params.get('candidateId')).toBe('candidate');
    expect(request.request.params.get('instructorId')).toBe('instructor');
    expect(request.request.params.get('status')).toBe('DONE');
    expect(request.request.params.get('circuitId')).toBe('circuit');
    expect(request.request.params.get('page')).toBe('1');
    request.flush({});
  });

  it('creates, updates, changes status and deletes sessions', () => {
    const body = {
      candidateId: 'candidate', instructorId: 'instructor', sessionDate: '2026-10-29',
      startTime: '09:00', endTime: '10:00', price: 25, notes: null,
    };
    service.create(body).subscribe();
    const create = http.expectOne('/api/driving-sessions');
    expect(create.request.method).toBe('POST');
    create.flush({});
    service.update('session', body).subscribe();
    const update = http.expectOne('/api/driving-sessions/session');
    expect(update.request.method).toBe('PUT');
    update.flush({});
    service.changeStatus('session', { status: 'DONE', notes: 'Completed' }).subscribe();
    const status = http.expectOne('/api/driving-sessions/session/status');
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ status: 'DONE', notes: 'Completed' });
    status.flush({});
    service.delete('session').subscribe();
    const remove = http.expectOne('/api/driving-sessions/session');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
});
