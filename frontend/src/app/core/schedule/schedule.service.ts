import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ScheduleAvailability, ScheduleDay } from './schedule.models';

@Injectable({ providedIn: 'root' })
export class ScheduleService {
  constructor(private readonly http: HttpClient) {}

  getSchedule(from: string, to: string, instructorId?: string): Observable<ScheduleDay[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (instructorId) params = params.set('instructorId', instructorId);
    return this.http.get<ScheduleDay[]>('/api/schedule', { params });
  }

  getMySchedule(): Observable<ScheduleDay[]> {
    return this.http.get<ScheduleDay[]>('/api/me/schedule');
  }

  getAvailability(date: string, startTime: string, endTime: string): Observable<ScheduleAvailability> {
    const params = new HttpParams()
      .set('date', date)
      .set('startTime', startTime)
      .set('endTime', endTime);
    return this.http.get<ScheduleAvailability>('/api/schedule/availability', { params });
  }
}
