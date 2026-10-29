export interface Skill {
  id: string;
  name: string;
  displayOrder: number;
}

export interface CandidateSkill {
  skillId: string;
  name: string;
  displayOrder: number;
  level: number;
  updatedBy: string | null;
  updatedAt: string | null;
}

export interface CandidateSkills {
  candidateId: string;
  overallProgress: number;
  skills: CandidateSkill[];
}

export interface UpdateCandidateSkillsRequest {
  skills: Array<{ skillId: string; level: number }>;
}
