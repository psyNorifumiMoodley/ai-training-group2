import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { CandidateListComponent } from './components/candidate-list/candidate-list.component';
import { MarkerListComponent } from './components/marker-list/marker-list.component';

const userManagementRoutes: Routes = [
  { path: '', component: CandidateListComponent, canActivate: [roleGuard], data: { role: 'ADMIN' } },
  { path: 'markers', component: MarkerListComponent, canActivate: [roleGuard], data: { role: 'ADMIN' } },
];

export default userManagementRoutes;
