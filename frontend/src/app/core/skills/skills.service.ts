import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CandidateSkills, Skill, UpdateCandidateSkillsRequest } from './skill.models';

@Injectable({ providedIn: 'root' })
export class SkillsService {
  constructor(private readonly http: HttpClient) {}

  getSkills(): Observable<Skill[]> {
    return this.http.get<Skill[]>('/api/skills');
  }

  getCandidateSkills(candidateId: string): Observable<CandidateSkills> {
    return this.http.get<CandidateSkills>(`/api/candidates/${candidateId}/skills`);
  }

  getMySkills(): Observable<CandidateSkills> {
    return this.http.get<CandidateSkills>('/api/me/skills');
  }

  updateCandidateSkills(
    candidateId: string,
    request: UpdateCandidateSkillsRequest,
  ): Observable<CandidateSkills> {
    return this.http.put<CandidateSkills>(`/api/candidates/${candidateId}/skills`, request);
  }
}
