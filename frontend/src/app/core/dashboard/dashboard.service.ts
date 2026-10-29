import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminDashboard, InstructorDashboard } from './dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly http: HttpClient) {}

  getAdminDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>('/api/dashboard');
  }

  getInstructorDashboard(): Observable<InstructorDashboard> {
    return this.http.get<InstructorDashboard>('/api/dashboard/instructor');
  }
}
