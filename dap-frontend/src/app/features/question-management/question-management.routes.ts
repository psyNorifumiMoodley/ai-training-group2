import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';

export const questionManagementRoutes: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'MARKER'] },
    loadComponent: () =>
      import('./components/question-list/question-list.component')
        .then(m => m.QuestionListComponent),
  },
];
