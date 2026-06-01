import { Routes } from '@angular/router';

export const assessmentGenerationRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/assessment-generate/assessment-generate.component')
        .then(m => m.AssessmentGenerateComponent),
  },
  {
    path: 'questions',
    loadComponent: () =>
      import('./components/question-selection/question-selection.component')
        .then(m => m.QuestionSelectionComponent),
  },
];
