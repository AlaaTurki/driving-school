import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Candidate,
  CandidatePage,
  CandidateSearch,
  SaveCandidateRequest,
  UpdateCandidateRequest,
  UpdateCandidateRolesRequest,
} from './candidate.models';

@Injectable({ providedIn: 'root' })
export class CandidateService {
  constructor(private readonly http: HttpClient) {}

  findAll(query: CandidateSearch): Observable<CandidatePage> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size);
    if (query.search.trim()) {
      params = params.set('search', query.search.trim());
    }
    if (query.status !== 'ALL') {
      params = params.set('status', query.status);
    }
    return this.http.get<CandidatePage>('/api/candidates', { params });
  }

  findById(id: string): Observable<Candidate> {
    return this.http.get<Candidate>(`/api/candidates/${id}`);
  }

  create(request: SaveCandidateRequest): Observable<Candidate> {
    return this.http.post<Candidate>('/api/candidates', request);
  }

  update(id: string, changes: UpdateCandidateRequest): Observable<Candidate> {
    return this.http.put<Candidate>(`/api/candidates/${id}`, changes);
  }

  updateRoles(id: string, changes: UpdateCandidateRolesRequest): Observable<Candidate> {
    return this.http.put<Candidate>(`/api/candidates/${id}/roles`, changes);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/candidates/${id}`);
  }
}
