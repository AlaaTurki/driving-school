import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Candidate,
  CandidatePage,
  CandidateSearch,
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

  update(id: string, changes: UpdateCandidateRequest): Observable<Candidate> {
    return this.http.put<Candidate>(`/api/candidates/${id}`, changes);
  }

  updateRoles(id: string, changes: UpdateCandidateRolesRequest): Observable<Candidate> {
    return this.http.put<Candidate>(`/api/candidates/${id}/roles`, changes);
  }
}
