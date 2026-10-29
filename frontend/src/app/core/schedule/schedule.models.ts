export type DrivingSessionStatus = 'PLANNED' | 'DONE' | 'CANCELLED' | 'ABSENT';

export interface ScheduleEntry {
  sessionId: string;
  instructorId: string;
  instructorName: string;
  startTime: string;
  endTime: string;
  candidateName: string;
  circuit: string | null;
  vehicle: string | null;
  status: DrivingSessionStatus;
}

export interface ScheduleDay {
  date: string;
  sessions: ScheduleEntry[];
}

export interface AvailableInstructor {
  id: string;
  name: string;
  email: string;
}

export interface AvailableVehicle {
  id: string;
  registrationNumber: string;
  brand: string;
  model: string;
  type: string;
}

export interface ScheduleAvailability {
  instructors: AvailableInstructor[];
  vehicles: AvailableVehicle[];
}
