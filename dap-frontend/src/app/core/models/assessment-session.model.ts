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
export interface CodingResponseRequest { code: string; }
export type ResponseRequest =
  | McqResponseRequest
  | McqPlusResponseRequest
  | TextResponseRequest
  | DocResponseRequest
  | GroupResponseRequest
  | CodingResponseRequest;

export interface TestCaseResult {
  testCaseId: string | null;
  passed: boolean;
  actualOutput: string | null;
  executionTimeMs: number | null;
  memoryUsedMb: number | null;
  errorMessage: string | null;
  ordinal: number;
}

export interface CodeExecuteResponse {
  results: TestCaseResult[];
  executedAt: string;
}
