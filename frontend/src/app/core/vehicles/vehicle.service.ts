import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SaveVehicleRequest, Vehicle, VehiclePage, VehicleSearch } from './vehicle.models';

@Injectable({ providedIn: 'root' })
export class VehicleService {
  constructor(private readonly http: HttpClient) {}

  findAll(query: VehicleSearch): Observable<VehiclePage> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.search.trim()) {
      params = params.set('search', query.search.trim());
    }
    if (query.active !== 'ALL') {
      params = params.set('active', query.active);
    }
    return this.http.get<VehiclePage>('/api/vehicles', { params });
  }

  create(request: SaveVehicleRequest): Observable<Vehicle> {
    return this.http.post<Vehicle>('/api/vehicles', request);
  }
}
