export type CandidateStatus = 'ACTIVE' | 'INACTIVE';

export interface Candidate {
  id: string;
  fullName: string;
  email: string;
  roles?: string[];
  phone: string;
  status: CandidateStatus;
  registeredAt: string;
}

export interface UpdateCandidateRequest {
  fullName: string;
  phone: string;
  status: CandidateStatus;
}

export type UserRole = 'ADMIN' | 'INSTRUCTOR' | 'CANDIDATE';

export interface UpdateCandidateRolesRequest {
  roles: UserRole[];
}
