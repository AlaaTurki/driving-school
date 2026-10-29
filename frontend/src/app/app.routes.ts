import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { LoginComponent } from './features/login/login.component';
import { RegisterComponent } from './features/register/register.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'mobile-app',
    loadComponent: () =>
      import('./features/mobile-app/mobile-app.component').then((module) => module.MobileAppComponent),
    canActivate: [authGuard, roleGuard('CANDIDATE')],
  },
  {
    path: '',
    loadComponent: () =>
      import('./shared/layout/app-shell.component').then((module) => module.AppShellComponent),
    canActivate: [authGuard, roleGuard('ADMIN', 'INSTRUCTOR')],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((module) => module.DashboardComponent),
      },
      {
        path: 'schedule',
        loadComponent: () =>
          import('./features/schedule/schedule.component').then((module) => module.ScheduleComponent),
      },
      {
        path: 'sessions',
        loadComponent: () =>
          import('./features/sessions/sessions.component').then((module) => module.SessionsComponent),
      },
      {
        path: 'my-progress',
        loadComponent: () =>
          import('./features/progression/progression.component').then((module) => module.ProgressionComponent),
      },
      {
        path: 'candidates/:candidateId/skills',
        loadComponent: () =>
          import('./features/progression/progression.component').then((module) => module.ProgressionComponent),
      },
      {
        path: 'candidates/:candidateId',
        loadComponent: () =>
          import('./features/candidates/candidate-detail.component').then((module) => module.CandidateDetailComponent),
      },
      {
        path: 'payments',
        loadComponent: () =>
          import('./features/payments/payments.component').then((module) => module.PaymentsComponent),
        canActivate: [roleGuard('ADMIN', 'INSTRUCTOR')],
      },
      {
        path: 'candidates',
        loadComponent: () =>
          import('./features/candidates/candidates.component').then((module) => module.CandidatesComponent),
      },
      {
        path: 'instructors',
        loadComponent: () =>
          import('./features/instructors/instructors.component').then((module) => module.InstructorsComponent),
        canActivate: [roleGuard('ADMIN')],
      },
      {
        path: 'vehicles',
        loadComponent: () =>
          import('./features/vehicles/vehicles.component').then((module) => module.VehiclesComponent),
      },
      {
        path: 'circuits',
        loadComponent: () =>
          import('./features/circuits/circuits.component').then((module) => module.CircuitsComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
