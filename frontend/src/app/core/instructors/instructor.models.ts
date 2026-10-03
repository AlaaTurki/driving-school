export type InstructorStatus = 'ACTIVE' | 'INACTIVE';

export interface Instructor {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  licenseNumber: string;
  status: InstructorStatus;
  createdAt: string;
  updatedAt: string;
}

export interface InstructorPage {
  content: Instructor[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface InstructorSearch {
  search: string;
  status: InstructorStatus | 'ALL';
  page: number;
  size: number;
}

export interface CreateInstructorRequest {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  licenseNumber: string;
  initialPassword: string;
}

export interface UpdateInstructorRequest {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  licenseNumber: string;
  status: InstructorStatus;
}
