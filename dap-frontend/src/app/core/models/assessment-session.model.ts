import { QuestionResponse } from './question.model';

export interface AssessmentAccessResponse {
  assessmentId: string;
  questions: QuestionResponse[];
  remainingSeconds: number;
  candidateToken: string;
}

export interface McqResponseRequest  { selectedAnswers: string[]; }
export interface TextResponseRequest { answer: string; }
export interface DocResponseRequest  { filePath: string; }
export interface GroupResponseRequest { childResponses: Record<string, ResponseRequest>; }
export type ResponseRequest =
  | McqResponseRequest
  | TextResponseRequest
  | DocResponseRequest
  | GroupResponseRequest;
