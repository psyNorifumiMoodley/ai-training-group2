import { QuestionResponse } from './question.model';

export interface AssessmentAccessResponse {
  assessmentId: string;
  questions: QuestionResponse[];
  remainingSeconds: number;
  candidateToken: string;
  alreadyStarted: boolean;
}

export interface McqResponseRequest  { selectedAnswers: string[]; }
export interface McqPlusResponseRequest { selectedAnswers: string[]; followUpAnswer: string; }
export interface TextResponseRequest { answer: string; }
export interface DocResponseRequest  { filePath: string; }
export interface GroupResponseRequest { childAnswers: string[]; }
export type ResponseRequest =
  | McqResponseRequest
  | McqPlusResponseRequest
  | TextResponseRequest
  | DocResponseRequest
  | GroupResponseRequest;
