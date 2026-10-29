import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FinancialService } from './financial.service';

describe('FinancialService', () => {
  let service: FinancialService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FinancialService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('uses the signed-in candidate endpoints for a personal statement', () => {
    service.getBalance().subscribe();
    service.getPayments(undefined, 0, 20).subscribe();
    service.getStatement({
      dateFrom: '2026-01-01',
      dateTo: '2026-01-31',
      status: 'DONE',
      page: 0,
      size: 20,
    }).subscribe();

    const balance = httpTesting.expectOne('/api/me/balance');
    expect(balance.request.method).toBe('GET');
    balance.flush({});

    const payments = httpTesting.expectOne('/api/me/payments?page=0&size=20');
    expect(payments.request.method).toBe('GET');
    payments.flush({});

    const statement = httpTesting.expectOne((request) =>
      request.url === '/api/me/statement'
      && request.params.get('dateFrom') === '2026-01-01'
      && request.params.get('dateTo') === '2026-01-31'
      && request.params.get('status') === 'DONE',
    );
    expect(statement.request.method).toBe('GET');
    statement.flush({});
  });

  it('loads a selected candidate balance and paged payment history', () => {
    service.getBalance('candidate-id').subscribe();
    service.getPayments('candidate-id', 2, 20).subscribe();

    const balance = httpTesting.expectOne('/api/candidates/candidate-id/balance');
    expect(balance.request.method).toBe('GET');
    balance.flush({});

    const payments = httpTesting.expectOne('/api/candidates/candidate-id/payments?page=2&size=20');
    expect(payments.request.method).toBe('GET');
    payments.flush({});
  });

  it('creates, updates, and deletes admin payment records', () => {
    const request = {
      candidateId: 'candidate-id',
      amount: 125,
      paymentDate: '2026-10-28',
      paymentMethod: 'CARD' as const,
      notes: null,
    };

    service.createPayment(request).subscribe();
    const create = httpTesting.expectOne('/api/payments');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(request);
    create.flush({});

    service.updatePayment('payment-id', request).subscribe();
    const update = httpTesting.expectOne('/api/payments/payment-id');
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(request);
    update.flush({});

    service.deletePayment('payment-id').subscribe();
    const remove = httpTesting.expectOne('/api/payments/payment-id');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });

  it('filters administrator payments by candidate, date range and payment method', () => {
    service.getAllPayments({
      candidateId: 'candidate-id',
      dateFrom: '2026-10-01',
      dateTo: '2026-10-31',
      paymentMethod: 'CARD',
      page: 2,
      size: 20,
    }).subscribe();
    const request = httpTesting.expectOne((req) => req.url === '/api/payments');
    expect(request.request.params.get('candidateId')).toBe('candidate-id');
    expect(request.request.params.get('dateFrom')).toBe('2026-10-01');
    expect(request.request.params.get('dateTo')).toBe('2026-10-31');
    expect(request.request.params.get('paymentMethod')).toBe('CARD');
    expect(request.request.params.get('page')).toBe('2');
    request.flush({});
  });
});
