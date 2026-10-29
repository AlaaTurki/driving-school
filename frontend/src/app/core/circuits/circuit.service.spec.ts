import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CircuitService } from './circuit.service';

describe('CircuitService', () => {
  let service: CircuitService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CircuitService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists filtered and paginated circuits', () => {
    service.findAll({ search: 'North', active: false, page: 1, size: 10 }).subscribe();
    const request = http.expectOne((req) => req.url === '/api/circuits');
    expect(request.request.params.get('search')).toBe('North');
    expect(request.request.params.get('active')).toBe('false');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 10, first: false, last: true });
  });

  it('creates, updates and deletes circuits through the API', () => {
    const body = { name: 'Centre', description: null, location: null, price: 12.5, active: true };
    service.create(body).subscribe();
    const create = http.expectOne('/api/circuits');
    expect(create.request.method).toBe('POST');
    create.flush({ id: 'circuit-id', ...body, createdAt: '', updatedAt: '' });

    service.update('circuit-id', { ...body, active: false }).subscribe();
    const update = http.expectOne('/api/circuits/circuit-id');
    expect(update.request.method).toBe('PUT');
    update.flush({ id: 'circuit-id', ...body, active: false, createdAt: '', updatedAt: '' });

    service.delete('circuit-id').subscribe();
    const remove = http.expectOne('/api/circuits/circuit-id');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
});
