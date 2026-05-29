export interface McqQuestionRequest {
  category: string;
  question: string;
  options: string[];
  correctAnswers: string[];
}

export interface TextQuestionRequest {
  category: string;
  question: string;
  keywords: string[];
}

export interface DocQuestionRequest {
  category: string;
  question: string;
}

export interface GroupQuestionRequest {
  category: string;
  question: string;
  ordered: boolean;
  followUpQuestionIds: string[];
}

export interface BaseQuestionResponse {
  id: string;
  category: string;
  question: string;
}

export interface McqQuestionResponse extends BaseQuestionResponse {
  options: string[];
  correctAnswers: string[];
}

export interface TextQuestionResponse extends BaseQuestionResponse {
  keywords: string[];
}

export interface DocQuestionResponse extends BaseQuestionResponse {}

export interface GroupQuestionResponse extends BaseQuestionResponse {
  ordered: boolean;
  followUpQuestions: TextQuestionResponse[];
}

export type QuestionResponse =
  | McqQuestionResponse
  | TextQuestionResponse
  | DocQuestionResponse
  | GroupQuestionResponse;
