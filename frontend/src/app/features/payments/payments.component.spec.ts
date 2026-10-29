import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { CandidateService } from '../../core/candidates/candidate.service';
import { FinancialService } from '../../core/financials/financial.service';
import { Payment } from '../../core/financials/financial.models';
import { PaymentsComponent } from './payments.component';

describe('PaymentsComponent form', () => {
  let financial: jasmine.SpyObj<FinancialService>;

  beforeEach(() => {
    financial = jasmine.createSpyObj<FinancialService>('FinancialService', [
      'getBalance', 'getPayments', 'getAllPayments', 'getStatement', 'createPayment', 'updatePayment', 'deletePayment',
    ]);
    financial.getBalance.and.returnValue(of({ candidateId: 'candidate-id', totalSessions: 0, totalPaid: 0, remaining: 0, credit: false }));
    financial.getPayments.and.returnValue(of(emptyPaymentPage()));
    financial.getAllPayments.and.returnValue(of(emptyPaymentPage()));
    financial.getStatement.and.returnValue(of(emptyStatement()));
    financial.createPayment.and.returnValue(of({} as Payment));
    const candidates = jasmine.createSpyObj<CandidateService>('CandidateService', ['findAll']);
    candidates.findAll.and.returnValue(of({
      content: [{
        id: 'candidate-id', firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com',
        userId: 'user-id', phone: '123', dateOfBirth: null, address: null, registrationDate: '2026-01-01',
        status: 'ACTIVE', notes: null, createdAt: '', updatedAt: '', roles: [],
      }],
      totalElements: 1, totalPages: 1, number: 0, size: 100, first: true, last: true,
    }));
    TestBed.configureTestingModule({
      imports: [PaymentsComponent],
      providers: [
        { provide: AuthService, useValue: { getSession: () => ({ roles: ['ADMIN'] }) } },
        { provide: CandidateService, useValue: candidates },
        { provide: FinancialService, useValue: financial },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => null } } } },
      ],
    });
  });

  it('submits a valid payment amount without a balance input', () => {
    const component = TestBed.createComponent(PaymentsComponent).componentInstance;
    Object.assign(component.paymentDraft, {
      amount: 25, paymentDate: '2026-10-29', paymentMethod: 'CASH', notes: '',
    });
    component.savePayment();

    expect(financial.createPayment).toHaveBeenCalledWith({
      candidateId: 'candidate-id', amount: 25, paymentDate: '2026-10-29', paymentMethod: 'CASH', notes: null,
    });
  });

  it('does not submit an invalid or non-positive payment amount', () => {
    const component = TestBed.createComponent(PaymentsComponent).componentInstance;
    component.paymentDraft.amount = 0;
    component.savePayment();

    expect(financial.createPayment).not.toHaveBeenCalled();
  });

  it('displays the balance summary returned by the balance API', () => {
    const apiBalance = {
      candidateId: 'candidate-id',
      totalSessions: 560,
      totalPaid: 250,
      remaining: 310,
      credit: false,
    };
    financial.getBalance.and.returnValue(of(apiBalance));

    const fixture = TestBed.createComponent(PaymentsComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.balance()).toEqual(apiBalance);
    expect(fixture.nativeElement.textContent).toContain('560,000 DT');
    expect(fixture.nativeElement.textContent).toContain('250,000 DT');
    expect(fixture.nativeElement.textContent).toContain('310,000 DT');
  });

  function emptyPaymentPage() {
    return { content: [], number: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true };
  }

  function emptyStatement() {
    return {
      candidateId: 'candidate-id', content: [], totalSessions: 0, totalPaid: 0, remaining: 0, credit: false,
      number: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true,
    };
  }
});
