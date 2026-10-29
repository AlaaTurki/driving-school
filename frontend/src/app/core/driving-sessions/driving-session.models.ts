export type DrivingSessionStatus = 'PLANNED' | 'DONE' | 'CANCELLED' | 'ABSENT';

export interface SessionPerson {
  id: string;
  name: string;
}

export interface SessionVehicle {
  id: string;
  registrationNumber: string;
  brand: string;
  model: string;
}

export interface SessionCircuit {
  id: string;
  name: string;
}

export interface DrivingSession {
  id: string;
  candidate: SessionPerson;
  instructor: SessionPerson;
  vehicle: SessionVehicle | null;
  circuit: SessionCircuit | null;
  sessionDate: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  durationHours: number;
  price: number;
  status: DrivingSessionStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  paidAmount?: number;
  remaining?: number;
}

export interface DrivingSessionPage {
  content: DrivingSession[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface DrivingSessionQuery {
  dateFrom?: string;
  dateTo?: string;
  candidateId?: string;
  instructorId?: string;
  status?: DrivingSessionStatus;
  circuitId?: string;
  page: number;
  size: number;
}

export interface SaveDrivingSessionRequest {
  candidateId: string;
  instructorId?: string | null;
  vehicleId?: string | null;
  circuitId?: string | null;
  sessionDate: string;
  startTime: string;
  endTime: string;
  price?: number | null;
  notes?: string | null;
}

export interface ChangeDrivingSessionStatusRequest {
  status: DrivingSessionStatus;
  notes?: string | null;
}
