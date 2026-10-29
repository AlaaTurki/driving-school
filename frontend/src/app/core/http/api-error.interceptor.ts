import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

function messageFor(error: HttpErrorResponse): string {
  if (error.status === 0) return 'Le serveur est injoignable. Vérifiez votre connexion.';
  if (typeof error.error?.message === 'string') return error.error.message;
  return 'Une erreur est survenue lors de la communication avec le serveur.';
}

function isDrivingSessionUrl(url: string): boolean {
  const pathname = new URL(url, 'http://localhost').pathname;
  return pathname === '/api/driving-sessions' || pathname.startsWith('/api/driving-sessions/');
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const snackbar = inject(MatSnackBar);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status !== 401
        && !(error.status === 409 && isDrivingSessionUrl(request.url))) {
        snackbar.open(messageFor(error), 'Fermer', { duration: 5000 });
      }
      return throwError(() => error);
    }),
  );
};
