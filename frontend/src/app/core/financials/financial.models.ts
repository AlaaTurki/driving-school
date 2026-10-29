export type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER' | 'OTHER';
export type DrivingSessionStatus = 'PLANNED' | 'DONE' | 'CANCELLED' | 'ABSENT';

export interface BalanceSummary {
  candidateId: string;
  totalSessions: number;
  totalPaid: number;
  remaining: number;
  credit: boolean;
}

export interface Payment {
  id: string;
  candidateId: string;
  candidateName: string;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentPage {
  content: Payment[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface StatementLine {
  sessionId: string;
  day: string;
  date: string;
  startTime: string;
  endTime: string;
  hours: number;
  price: number;
  status: DrivingSessionStatus;
  paidAmount: number;
  remaining: number;
}

export interface StatementPage {
  candidateId: string;
  content: StatementLine[];
  totalSessions: number;
  totalPaid: number;
  remaining: number;
  credit: boolean;
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface StatementQuery {
  candidateId?: string;
  dateFrom?: string;
  dateTo?: string;
  status?: DrivingSessionStatus;
  page: number;
  size: number;
}

export interface PaymentQuery {
  candidateId?: string;
  dateFrom?: string;
  dateTo?: string;
  paymentMethod?: PaymentMethod;
  page: number;
  size: number;
}

export interface SavePaymentRequest {
  candidateId: string;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  notes: string | null;
}
