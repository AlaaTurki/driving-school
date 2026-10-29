import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Circuit, CircuitPage, CircuitSearch, SaveCircuitRequest } from './circuit.models';

@Injectable({ providedIn: 'root' })
export class CircuitService {
  constructor(private readonly http: HttpClient) {}

  findAll(query: CircuitSearch): Observable<CircuitPage> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.search.trim()) params = params.set('search', query.search.trim());
    if (query.active !== 'ALL') params = params.set('active', query.active);
    return this.http.get<CircuitPage>('/api/circuits', { params });
  }

  create(request: SaveCircuitRequest): Observable<Circuit> {
    return this.http.post<Circuit>('/api/circuits', request);
  }

  update(id: string, request: SaveCircuitRequest): Observable<Circuit> {
    return this.http.put<Circuit>(`/api/circuits/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/circuits/${id}`);
  }
}
