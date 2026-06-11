export interface McqAnswerPayload {
  selectedAnswers: string[];
  allOptions: string[];
  correctAnswers: string[];
}

export interface TextAnswerPayload {
  answer: string;
}

export interface DocAnswerPayload {
  fileName: string;
}

export interface AssessmentSummaryResponse {
  id: string;
  candidateName: string;
  role: string;
  bankName: string;
  submittedAt: string;
  assignedDate: string;
  status: string;
  timeLimitMinutes: number;
}

export interface ResponseReviewItem {
  responseId: string | null;
  questionId: string;
  questionBody: string;
  questionType: 'MCQ' | 'TEXT' | 'DOC' | 'GROUP';
  answer: McqAnswerPayload | TextAnswerPayload | DocAnswerPayload;
  correct: boolean | null;
  feedbackDraft: string;
  marks: number;
  score: number | null;
  childItems?: ResponseReviewItem[];
}

export interface FeedbackUpdateRequest {
  feedbackText: string;
}

export interface FeedbackItem {
  questionId: string;
  questionBody: string;
  feedbackText: string;
}
