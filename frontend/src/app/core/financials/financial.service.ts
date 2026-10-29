import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BalanceSummary,
  Payment,
  PaymentPage,
  PaymentQuery,
  SavePaymentRequest,
  StatementPage,
  StatementQuery,
} from './financial.models';

@Injectable({ providedIn: 'root' })
export class FinancialService {
  constructor(private readonly http: HttpClient) {}

  getBalance(candidateId?: string): Observable<BalanceSummary> {
    const url = candidateId ? `/api/candidates/${candidateId}/balance` : '/api/me/balance';
    return this.http.get<BalanceSummary>(url);
  }

  getPayments(candidateId: string | undefined, page: number, size: number): Observable<PaymentPage> {
    const url = candidateId ? `/api/candidates/${candidateId}/payments` : '/api/me/payments';
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PaymentPage>(url, { params });
  }

  getAllPayments(query: PaymentQuery): Observable<PaymentPage> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.candidateId) params = params.set('candidateId', query.candidateId);
    if (query.dateFrom) params = params.set('dateFrom', query.dateFrom);
    if (query.dateTo) params = params.set('dateTo', query.dateTo);
    if (query.paymentMethod) params = params.set('paymentMethod', query.paymentMethod);
    return this.http.get<PaymentPage>('/api/payments', { params });
  }

  getStatement(query: StatementQuery): Observable<StatementPage> {
    const url = query.candidateId
      ? `/api/candidates/${query.candidateId}/statement`
      : '/api/me/statement';
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.dateFrom) params = params.set('dateFrom', query.dateFrom);
    if (query.dateTo) params = params.set('dateTo', query.dateTo);
    if (query.status) params = params.set('status', query.status);
    return this.http.get<StatementPage>(url, { params });
  }

  createPayment(request: SavePaymentRequest): Observable<Payment> {
    return this.http.post<Payment>('/api/payments', request);
  }

  updatePayment(id: string, request: SavePaymentRequest): Observable<Payment> {
    return this.http.put<Payment>(`/api/payments/${id}`, request);
  }

  deletePayment(id: string): Observable<void> {
    return this.http.delete<void>(`/api/payments/${id}`);
  }
}
