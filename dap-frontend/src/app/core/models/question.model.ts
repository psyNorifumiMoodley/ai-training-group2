export type QuestionType = 'MCQ' | 'TEXT' | 'DOC' | 'GROUP';

export interface McqQuestionRequest {
  type: 'MCQ';
  category: string;
  question: string;
  options: string[];
  correctAnswers: string[];
}

export interface TextQuestionRequest {
  type: 'TEXT';
  category: string;
  question: string;
  keywords: string[];
}

export interface DocQuestionRequest {
  type: 'DOC';
  category: string;
  question: string;
}

export interface GroupQuestionRequest {
  type: 'GROUP';
  category: string;
  question: string;
  ordered: boolean;
  followUpQuestionIds: string[];
}

export interface BaseQuestionResponse {
  type: QuestionType;
  id: string;
  category: string;
  question: string;
}

export interface McqQuestionResponse extends BaseQuestionResponse {
  type: 'MCQ';
  options: string[];
  correctAnswers: string[];
}

export interface TextQuestionResponse extends BaseQuestionResponse {
  type: 'TEXT';
  keywords: string[];
}

export interface DocQuestionResponse extends BaseQuestionResponse {
  type: 'DOC';
}

export interface GroupQuestionResponse extends BaseQuestionResponse {
  type: 'GROUP';
  ordered: boolean;
  followUpQuestions: TextQuestionResponse[];
}

export type QuestionResponse =
  | McqQuestionResponse
  | TextQuestionResponse
  | DocQuestionResponse
  | GroupQuestionResponse;
