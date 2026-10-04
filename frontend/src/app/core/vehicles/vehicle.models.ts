export type VehicleType = 'MANUAL' | 'AUTOMATIC' | 'MOTORCYCLE' | 'TRUCK';

export interface Vehicle {
  id: string;
  registrationNumber: string;
  brand: string;
  model: string;
  type: VehicleType;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VehiclePage {
  content: Vehicle[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface VehicleSearch {
  search: string;
  active: boolean | 'ALL';
  page: number;
  size: number;
}

export interface SaveVehicleRequest {
  registrationNumber: string;
  brand: string;
  model: string;
  type: VehicleType;
  active: boolean;
}
