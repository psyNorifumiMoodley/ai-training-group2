import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { CandidateListComponent } from './components/candidate-list/candidate-list.component';
import { MarkerListComponent } from './components/marker-list/marker-list.component';

const userManagementRoutes: Routes = [
  { path: '', component: CandidateListComponent, canActivate: [roleGuard], data: { roles: ['ADMIN', 'MARKER'] } },
  { path: 'markers', component: MarkerListComponent, canActivate: [roleGuard], data: { roles: ['ADMIN'] } },
];

export default userManagementRoutes;
