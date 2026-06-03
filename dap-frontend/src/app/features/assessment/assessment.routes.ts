import { Routes } from '@angular/router';

export const assessmentRoutes: Routes = [
  {
    path: 'access/:token',
    loadComponent: () =>
      import('./components/assessment-access/assessment-access.component')
        .then(m => m.AssessmentAccessComponent),
  },
  {
    path: ':assessmentId/take',
    loadComponent: () =>
      import('./components/assessment-taking/assessment-taking.component')
        .then(m => m.AssessmentTakingComponent),
  },
  {
    path: ':assessmentId/confirmation',
    loadComponent: () =>
      import('./components/assessment-confirmation/assessment-confirmation.component')
        .then(m => m.AssessmentConfirmationComponent),
  },
];
