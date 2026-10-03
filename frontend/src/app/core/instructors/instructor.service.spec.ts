import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  CreateInstructorRequest,
  Instructor,
  InstructorPage,
  UpdateInstructorRequest,
} from './instructor.models';
import { InstructorService } from './instructor.service';

describe('InstructorService', () => {
  let service: InstructorService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InstructorService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads a filtered instructor page', () => {
    const page: InstructorPage = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 20,
      first: true,
      last: true,
    };

    service.findAll({ search: 'license', status: 'INACTIVE', page: 0, size: 20 })
      .subscribe((result) => expect(result).toEqual(page));
    const request = httpTesting.expectOne((req) => req.url === '/api/instructors');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('license');
    expect(request.request.params.get('status')).toBe('INACTIVE');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    request.flush(page);
  });

  it('creates an instructor with the initial password', () => {
    const payload: CreateInstructorRequest = {
      firstName: 'First',
      lastName: 'Last',
      phone: '+21612345678',
      email: 'instructor@example.com',
      licenseNumber: 'LIC-123',
      initialPassword: 'initial-password',
    };

    service.create(payload).subscribe((result) => expect(result.email).toBe(payload.email));
    const request = httpTesting.expectOne('/api/instructors');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(instructor());
  });

  it('updates instructor contact details and status without a password', () => {
    const payload: UpdateInstructorRequest = {
      firstName: 'Updated',
      lastName: 'Last',
      phone: '+21698765432',
      email: 'updated@example.com',
      licenseNumber: 'LIC-456',
      status: 'INACTIVE',
    };

    service.update('instructor-id', payload).subscribe((result) => {
      expect(result.status).toBe('INACTIVE');
    });
    const request = httpTesting.expectOne('/api/instructors/instructor-id');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);
    expect(request.request.body.initialPassword).toBeUndefined();
    request.flush({ ...instructor(), ...payload });
  });

  it('deletes an instructor and its account through the API', () => {
    service.delete('instructor-id').subscribe();
    const request = httpTesting.expectOne('/api/instructors/instructor-id');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  function instructor(): Instructor {
    return {
      id: 'instructor-id',
      userId: 'user-id',
      firstName: 'First',
      lastName: 'Last',
      phone: '+21612345678',
      email: 'instructor@example.com',
      licenseNumber: 'LIC-123',
      status: 'ACTIVE',
      createdAt: '2026-10-03T10:00:00Z',
      updatedAt: '2026-10-03T10:00:00Z',
    };
  }
});
