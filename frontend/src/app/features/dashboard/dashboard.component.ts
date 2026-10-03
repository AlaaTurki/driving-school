import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthSession } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  readonly session: AuthSession | null;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.session = this.authService.getSession();
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
