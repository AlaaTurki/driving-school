import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export function roleGuard(...allowedRoles: string[]): CanActivateFn {
  return () => {
    const router = inject(Router);
    const session = inject(AuthService).getSession();
    if (!session) return router.createUrlTree(['/login']);
    if (session.roles.includes('CANDIDATE') && !allowedRoles.includes('CANDIDATE')) {
      return router.createUrlTree(['/mobile-app']);
    }
    if (session.roles.some((role) => allowedRoles.includes(role))) return true;
    if (session.roles.includes('CANDIDATE')) return router.createUrlTree(['/mobile-app']);
    return router.createUrlTree(['/dashboard']);
  };
}
