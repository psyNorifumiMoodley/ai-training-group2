import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'admin/users',
    loadChildren: () => import('./features/user-management/user-management.routes'),
  },
];
