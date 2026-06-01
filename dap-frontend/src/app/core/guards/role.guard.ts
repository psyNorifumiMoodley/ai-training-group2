import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/auth.model';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as Role[];
  if (requiredRoles.some(role => auth.hasRole(role))) {
    return true;
  }

  const user = auth.currentUser();
  if (user?.role === 'ADMIN' || user?.role === 'MARKER') {
    return router.createUrlTree(['/dashboard']);
  }
  return router.createUrlTree(['/login']);
};
