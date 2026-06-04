export interface McqAnswerPayload {
  selectedAnswers: string[];
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
  responseId: string;
  questionId: string;
  questionBody: string;
  questionType: 'MCQ' | 'TEXT' | 'DOC';
  answer: McqAnswerPayload | TextAnswerPayload | DocAnswerPayload;
  correct: boolean | null;
  feedbackDraft: string;
  marks: number;
  score: number | null;
}

export interface FeedbackUpdateRequest {
  feedbackText: string;
}

export interface FeedbackItem {
  questionId: string;
  questionBody: string;
  feedbackText: string;
}
