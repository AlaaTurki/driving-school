export type CandidateStatus = 'ACTIVE' | 'INACTIVE' | 'COMPLETED' | 'SUSPENDED';

export interface Candidate {
  id: string;
  userId: string | null;
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  dateOfBirth: string | null;
  address: string | null;
  registrationDate: string;
  status: CandidateStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  roles: string[];
}

export interface CandidatePage {
  content: Candidate[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface CandidateSearch {
  search: string;
  status: CandidateStatus | 'ALL';
  page: number;
  size: number;
}

export type UserRole = 'ADMIN' | 'INSTRUCTOR' | 'CANDIDATE';

export interface UpdateCandidateRolesRequest {
  roles: UserRole[];
}
