export type CandidateStatus = 'ACTIVE' | 'INACTIVE';

export interface Candidate {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  status: CandidateStatus;
  registeredAt: string;
}

export interface UpdateCandidateRequest {
  fullName: string;
  phone: string;
  status: CandidateStatus;
}
