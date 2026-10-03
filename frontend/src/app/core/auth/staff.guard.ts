import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const staffGuard: CanActivateFn = () => {
  const router = inject(Router);
  const session = inject(AuthService).getSession();
  if (session?.roles.some((role) => role === 'ADMIN' || role === 'INSTRUCTOR')) {
    return true;
  }

  return router.createUrlTree([session ? '/dashboard' : '/login']);
};
