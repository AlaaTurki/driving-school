import { Component, HostListener, computed, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from '../../core/auth/auth.service';

interface MenuItem {
  label: string;
  icon: string;
  route?: string;
  roles: string[];
  comingSoon?: boolean;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatButtonModule, MatIconModule, MatSidenavModule, MatToolbarModule],
  template: `
    <mat-sidenav-container class="shell-container">
      <mat-sidenav #sidenav class="shell-sidenav" [mode]="mobile() ? 'over' : 'side'" [opened]="!mobile()">
        <a class="brand" routerLink="/dashboard" (click)="closeOnMobile(sidenav)">
          <span class="brand-icon"><mat-icon>directions_car</mat-icon></span>
          <span class="brand-copy"><strong>Conduite</strong><small>ESPACE DE GESTION</small></span>
        </a>
        <div class="school-switcher">
          <span class="school-avatar">C</span>
          <span class="school-copy"><strong>Mon auto-école</strong><small>Établissement</small></span>
        </div>
        <p class="nav-label">MENU PRINCIPAL</p>
        <nav aria-label="Navigation principale">
          @for (item of visibleMenu(); track item.label) {
            @if (item.route) {
              <a class="nav-link" [routerLink]="item.route" routerLinkActive="active"
                 ariaCurrentWhenActive="page" (click)="closeOnMobile(sidenav)">
                <mat-icon>{{ item.icon }}</mat-icon><span>{{ item.label }}</span>
              </a>
            } @else {
              <span class="nav-link disabled" aria-disabled="true">
                <mat-icon>{{ item.icon }}</mat-icon><span>{{ item.label }}</span><small>Bientôt</small>
              </span>
            }
          }
        </nav>
        <div class="sidebar-bottom">
          <span class="avatar">{{ initial() }}</span>
          <span class="account-details"><strong>{{ email() }}</strong><small>{{ roleLabel() }}</small></span>
        </div>
      </mat-sidenav>
      <mat-sidenav-content>
        <mat-toolbar class="shell-toolbar">
          @if (mobile()) {
            <button mat-icon-button type="button" aria-label="Ouvrir le menu" (click)="sidenav.toggle()">
              <mat-icon>menu</mat-icon>
            </button>
          }
          <span class="toolbar-title">Espace de gestion</span>
          <span class="toolbar-spacer"></span>
          <span class="toolbar-email">{{ email() }}</span>
          <button mat-button type="button" (click)="logout()"><mat-icon>logout</mat-icon><span>Déconnexion</span></button>
        </mat-toolbar>
        <main class="shell-content"><router-outlet /></main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent {
  readonly roles = signal<string[]>([]);
  readonly email = signal('');
  readonly initial = computed(() => this.email().charAt(0).toUpperCase());
  readonly roleLabel = computed(() => this.roles().join(' · '));
  readonly mobile = signal(typeof window !== 'undefined' && window.innerWidth < 900);
  readonly visibleMenu = computed(() => this.menu.filter((item) =>
    item.roles.some((role) => this.roles().includes(role))));

  private readonly menu: MenuItem[] = [
    { label: 'Dashboard', icon: 'space_dashboard', route: '/dashboard', roles: ['ADMIN', 'INSTRUCTOR'] },
    { label: 'Candidates', icon: 'groups', route: '/candidates', roles: ['ADMIN'] },
    { label: 'Mes candidats', icon: 'group', route: '/candidates', roles: ['INSTRUCTOR'] },
    { label: 'Instructors', icon: 'school', route: '/instructors', roles: ['ADMIN'] },
    { label: 'Vehicles', icon: 'directions_car', route: '/vehicles', roles: ['ADMIN'] },
    { label: 'Circuits', icon: 'alt_route', route: '/circuits', roles: ['ADMIN', 'INSTRUCTOR'] },
    { label: 'Sessions', icon: 'event_note', route: '/sessions', roles: ['ADMIN', 'INSTRUCTOR'] },
    { label: 'Payments', icon: 'payments', route: '/payments', roles: ['ADMIN', 'INSTRUCTOR'] },
    { label: 'Schedule', icon: 'calendar_month', route: '/schedule', roles: ['ADMIN', 'INSTRUCTOR'] },
  ];

  constructor(private readonly authService: AuthService, private readonly router: Router) {
    const session = this.authService.getSession();
    this.roles.set(session?.roles ?? []);
    this.email.set(session?.email ?? '');
  }

  @HostListener('window:resize')
  onResize(): void {
    this.mobile.set(window.innerWidth < 900);
  }

  closeOnMobile(sidenav: { close: () => void }): void {
    if (this.mobile()) sidenav.close();
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

}
