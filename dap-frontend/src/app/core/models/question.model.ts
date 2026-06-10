export type QuestionType = 'MCQ' | 'MCQ_PLUS' | 'TEXT' | 'DOC' | 'GROUP' | 'CODING';

export type CodingQuestionLanguage = 'JAVA' | 'PYTHON' | 'CSHARP';

export interface QuestionBankResponse {
  id: string;
  name: string;
}

export interface TestCaseRequest {
  input: string;
  expectedOutput: string;
  timeoutSeconds: number;
  memoryMb: number;
}

export interface TestCase {
  id: string;
  input: string;
  expectedOutput: string;
  timeoutSeconds: number;
  memoryMb: number;
  ordinal: number;
}

export interface GroupChildRequest {
  questionText: string;
  keywords?: string[];
  marks: number;
}

export interface GroupChildResponse {
  id: string;
  questionText: string;
  keywords: string[];
  marks: number;
}

export interface McqQuestionRequest {
  type: 'MCQ';
  questionBankIds: string[];
  question: string;
  options: string[];
  correctAnswers: string[];
}

export interface McqPlusQuestionRequest {
  type: 'MCQ_PLUS';
  questionBankIds: string[];
  question: string;
  options: string[];
  correctAnswers: string[];
  followUpQuestion: string;
  followUpKeywords?: string[];
  followUpMarks: number;
}

export interface TextQuestionRequest {
  type: 'TEXT';
  questionBankIds: string[];
  question: string;
  keywords: string[];
  marks: number;
}

export interface DocQuestionRequest {
  type: 'DOC';
  questionBankIds: string[];
  question: string;
  marks: number;
}

export interface GroupQuestionRequest {
  type: 'GROUP';
  questionBankIds: string[];
  question: string;
  ordered: boolean;
  children: GroupChildRequest[];
}

export interface CodingQuestionRequest {
  type: 'CODING';
  questionBankIds: string[];
  question: string;
  language: CodingQuestionLanguage;
  testCases?: TestCaseRequest[];
}

export interface BaseQuestionResponse {
  type: QuestionType;
  id: string;
  questionBanks: QuestionBankResponse[];
  question: string;
}

export interface McqQuestionResponse extends BaseQuestionResponse {
  type: 'MCQ';
  options: string[];
  correctAnswers: string[];
  multiCorrect: boolean;
}

export interface McqPlusQuestionResponse extends BaseQuestionResponse {
  type: 'MCQ_PLUS';
  options: string[];
  correctAnswers: string[];
  multiCorrect: boolean;
  followUpQuestion: string;
  followUpKeywords: string[];
  followUpMarks: number;
  totalMarks: number;
}

export interface TextQuestionResponse extends BaseQuestionResponse {
  type: 'TEXT';
  keywords: string[];
  marks: number;
}

export interface DocQuestionResponse extends BaseQuestionResponse {
  type: 'DOC';
  marks: number;
}

export interface GroupQuestionResponse extends BaseQuestionResponse {
  type: 'GROUP';
  ordered: boolean;
  children: GroupChildResponse[];
  totalMarks: number;
}

export interface CodingQuestionResponse extends BaseQuestionResponse {
  type: 'CODING';
  language: CodingQuestionLanguage;
  testCases: TestCase[];
}

export type QuestionResponse =
  | McqQuestionResponse
  | McqPlusQuestionResponse
  | TextQuestionResponse
  | DocQuestionResponse
  | GroupQuestionResponse
  | CodingQuestionResponse;

export type QuestionRequest =
  | McqQuestionRequest
  | McqPlusQuestionRequest
  | TextQuestionRequest
  | DocQuestionRequest
  | GroupQuestionRequest
  | CodingQuestionRequest;
