import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ScheduleService } from './schedule.service';

describe('ScheduleService', () => {
  let service: ScheduleService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ScheduleService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the selected date range and optional instructor', () => {
    service.getSchedule('2026-10-26', '2026-11-01', 'instructor-1').subscribe();

    const request = http.expectOne((candidate) =>
      candidate.url === '/api/schedule'
      && candidate.params.get('from') === '2026-10-26'
      && candidate.params.get('to') === '2026-11-01'
      && candidate.params.get('instructorId') === 'instructor-1',
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('requests a candidate schedule from the authenticated-user endpoint', () => {
    service.getMySchedule().subscribe();

    const request = http.expectOne('/api/me/schedule');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('requests availability for the selected time slot', () => {
    service.getAvailability('2026-10-28', '10:00', '12:00').subscribe();

    const request = http.expectOne((candidate) =>
      candidate.url === '/api/schedule/availability'
      && candidate.params.get('date') === '2026-10-28'
      && candidate.params.get('startTime') === '10:00'
      && candidate.params.get('endTime') === '12:00',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ instructors: [], vehicles: [] });
  });
});
