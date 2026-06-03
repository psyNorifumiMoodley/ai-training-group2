export interface AssessmentSummaryResponse {
  id: string;
  candidateName: string;
  submittedAt: string;
  status: string;
}

export interface ResponseReviewItem {
  responseId: string;
  questionId: string;
  questionBody: string;
  questionType: string;
  answer: unknown;
  correct: boolean | null;
  feedbackDraft: string;
}

export interface FeedbackUpdateRequest {
  feedbackText: string;
}

export interface FeedbackItem {
  questionId: string;
  questionBody: string;
  feedbackText: string;
}
