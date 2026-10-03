import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Candidate, UpdateCandidateRequest, UpdateCandidateRolesRequest } from './candidate.models';

@Injectable({ providedIn: 'root' })
export class CandidateService {
  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Candidate[]> {
    return this.http.get<Candidate[]>('/api/candidates');
  }

  update(id: string, changes: UpdateCandidateRequest): Observable<Candidate> {
    return this.http.put<Candidate>(`/api/candidates/${id}`, changes);
  }

  updateRoles(id: string, changes: UpdateCandidateRolesRequest): Observable<Candidate> {
    return this.http.put<Candidate>(`/api/candidates/${id}/roles`, changes);
  }
}
