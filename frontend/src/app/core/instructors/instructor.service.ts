import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateInstructorRequest,
  Instructor,
  InstructorPage,
  InstructorSearch,
  UpdateInstructorRequest,
} from './instructor.models';

@Injectable({ providedIn: 'root' })
export class InstructorService {
  constructor(private readonly http: HttpClient) {}

  findAll(query: InstructorSearch): Observable<InstructorPage> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size);
    if (query.search.trim()) {
      params = params.set('search', query.search.trim());
    }
    if (query.status !== 'ALL') {
      params = params.set('status', query.status);
    }
    return this.http.get<InstructorPage>('/api/instructors', { params });
  }

  create(request: CreateInstructorRequest): Observable<Instructor> {
    return this.http.post<Instructor>('/api/instructors', request);
  }

  update(id: string, request: UpdateInstructorRequest): Observable<Instructor> {
    return this.http.put<Instructor>(`/api/instructors/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/instructors/${id}`);
  }
}
