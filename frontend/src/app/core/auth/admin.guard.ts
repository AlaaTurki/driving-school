import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const session = inject(AuthService).getSession();
  if (session?.roles.includes('ADMIN')) {
    return true;
  }

  return router.createUrlTree([session ? '/dashboard' : '/login']);
};
