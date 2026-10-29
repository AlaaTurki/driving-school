import { DrivingSessionStatus } from '../driving-sessions/driving-session.models';

export interface DashboardChartPoint {
  label: string;
  value: number;
  height: number;
}

export interface DashboardStatusCount extends DashboardChartPoint {
  status: DrivingSessionStatus;
}

export interface DashboardInstructorSession {
  id: string;
  candidate: { id: string; name: string };
  circuit: { id: string; name: string } | null;
  sessionDate: string;
  startTime: string;
  endTime: string;
  status: DrivingSessionStatus;
}

export interface AdminDashboard {
  candidatesCount: number;
  instructorsCount: number;
  sessionsToday: number;
  sessionsThisWeek: number;
  sessionsDone: number;
  sessionsPlanned: number;
  totalPaid: number;
  totalRemaining: number;
  sessionsPerDay: DashboardChartPoint[];
  paymentsPerMonth: DashboardChartPoint[];
  sessionsByStatus: DashboardStatusCount[];
}

export interface InstructorDashboard {
  todaySessions: DashboardInstructorSession[];
  upcomingSessions: DashboardInstructorSession[];
  candidatesCount: number;
  hoursTaught: {
    total: number;
    currentMonth: number;
  };
}
