export type AssessmentStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'MARKED';

export interface AssessmentRequest {
  candidateId: string;
  questionIds: string[];
  timeLimitMinutes: number;
}

export interface AssessmentResponse {
  id: string;
  candidateId: string;
  status: AssessmentStatus;
  invitationLink: string;
  timeLimitMinutes: number;
  createdAt: string;
}
export type QuestionType = 'MCQ' | 'TEXT' | 'DOC';
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface Assessment {
  id: string;
  candidateName: string;
  candidateInitials: string;
  role: string;
  bankName: string;
  status: AssessmentStatus;
  assignedDate: string;
  timeLimitMinutes: number;
}

export interface Question {
  id: string;
  type: QuestionType;
  bankId: string;
  questionText: string;
  options?: string[];
  correctOptionIndexes?: number[];
  marks: number;
  difficulty: Difficulty;
  tags: string[];
}

export interface QuestionResponse {
  questionId: string;
  type: QuestionType;
  selectedOptionIndexes?: number[];
  textAnswer?: string;
  fileName?: string;
  autoMarked?: boolean;
  autoScore?: number;
  markerScore?: number;
  markerComment?: string;
}

export interface QuestionBank {
  id: string;
  name: string;
  description: string;
  questionCount: number;
}

export interface NewQuestionRequest {
  type: QuestionType;
  bankId: string;
  questionText: string;
  options?: string[];
  correctOptionIndexes?: number[];
  marks: number;
  difficulty: Difficulty;
  tags: string[];
}
