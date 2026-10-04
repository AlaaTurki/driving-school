import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SaveVehicleRequest, VehiclePage } from './vehicle.models';
import { VehicleService } from './vehicle.service';

describe('VehicleService', () => {
  let service: VehicleService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(VehicleService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('lists vehicles using the API search, active filter and pagination', () => {
    const page: VehiclePage = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 20,
      first: true,
      last: true,
    };

    service.findAll({ search: ' ABC ', active: true, page: 0, size: 20 })
      .subscribe((result) => expect(result).toEqual(page));

    const request = httpTesting.expectOne((req) => req.url === '/api/vehicles');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('ABC');
    expect(request.request.params.get('active')).toBe('true');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    request.flush(page);
  });

  it('creates an active vehicle', () => {
    const body: SaveVehicleRequest = {
      registrationNumber: 'ABC-123',
      brand: 'Toyota',
      model: 'Yaris',
      type: 'MANUAL',
      active: true,
    };

    service.create(body).subscribe((result) => expect(result.registrationNumber).toBe('ABC-123'));
    const request = httpTesting.expectOne('/api/vehicles');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({
      id: 'vehicle-id',
      ...body,
      createdAt: '2026-10-04T00:00:00Z',
      updatedAt: '2026-10-04T00:00:00Z',
    });
  });
});
