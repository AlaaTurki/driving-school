import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ChangeDrivingSessionStatusRequest,
  DrivingSession,
  DrivingSessionPage,
  DrivingSessionQuery,
  SaveDrivingSessionRequest,
} from './driving-session.models';

@Injectable({ providedIn: 'root' })
export class DrivingSessionService {
  constructor(private readonly http: HttpClient) {}

  findAll(query: DrivingSessionQuery): Observable<DrivingSessionPage> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.dateFrom) params = params.set('dateFrom', query.dateFrom);
    if (query.dateTo) params = params.set('dateTo', query.dateTo);
    if (query.candidateId) params = params.set('candidateId', query.candidateId);
    if (query.instructorId) params = params.set('instructorId', query.instructorId);
    if (query.status) params = params.set('status', query.status);
    if (query.circuitId) params = params.set('circuitId', query.circuitId);
    return this.http.get<DrivingSessionPage>('/api/driving-sessions', { params });
  }

  create(request: SaveDrivingSessionRequest): Observable<DrivingSession> {
    return this.http.post<DrivingSession>('/api/driving-sessions', request);
  }

  update(id: string, request: SaveDrivingSessionRequest): Observable<DrivingSession> {
    return this.http.put<DrivingSession>(`/api/driving-sessions/${id}`, request);
  }

  changeStatus(id: string, request: ChangeDrivingSessionStatusRequest): Observable<DrivingSession> {
    return this.http.patch<DrivingSession>(`/api/driving-sessions/${id}/status`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/driving-sessions/${id}`);
  }
}
