export interface Circuit {
  id: string;
  name: string;
  description: string | null;
  location: string | null;
  price: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CircuitPage {
  content: Circuit[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface CircuitSearch {
  search: string;
  active: boolean | 'ALL';
  page: number;
  size: number;
}

export interface SaveCircuitRequest {
  name: string;
  description: string | null;
  location: string | null;
  price: number;
  active: boolean;
}
