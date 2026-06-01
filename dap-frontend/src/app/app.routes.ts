import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/components/login/login.component')
        .then(m => m.LoginComponent),
  },
  {
    path: 'assessment/:token',
    loadComponent: () =>
      import('./features/assessments/candidate/candidate-assessment.component')
        .then(m => m.CandidateAssessmentComponent),
  },
  {
    path: '',
    loadComponent: () =>
      import('./features/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'MARKER'] },
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent),
      },
      {
        path: 'candidates',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'MARKER'] },
        loadComponent: () =>
          import('./features/user-management/components/candidate-list/candidate-list.component')
            .then(m => m.CandidateListComponent),
      },
      {
        path: 'assessments',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'MARKER'] },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/assessments/list/assessment-list.component')
                .then(m => m.AssessmentListComponent),
          },
          {
            path: ':assessmentId/marking',
            canActivate: [roleGuard],
            data: { roles: ['MARKER'] },
            loadComponent: () =>
              import('./features/assessments/marking/marking.component')
                .then(m => m.MarkingComponent),
          },
        ],
      },
      {
        path: 'question-banks',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'MARKER'] },
        loadComponent: () =>
          import('./features/question-banks/bank-list/bank-list.component')
            .then(m => m.BankListComponent),
      },
      {
        path: 'questions',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'MARKER'] },
        loadChildren: () =>
          import('./features/question-management/question-management.routes')
            .then(m => m.questionManagementRoutes),
      },
      {
        path: 'markers',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/user-management/components/marker-list/marker-list.component')
            .then(m => m.MarkerListComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
