import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { staffGuard } from './core/auth/staff.guard';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LoginComponent } from './features/login/login.component';
import { RegisterComponent } from './features/register/register.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'candidates',
    loadComponent: () =>
      import('./features/candidates/candidates.component').then((module) => module.CandidatesComponent),
    canActivate: [authGuard, staffGuard],
  },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'dashboard' },
];
