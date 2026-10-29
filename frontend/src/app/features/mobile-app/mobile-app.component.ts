import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-mobile-app',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <main class="mobile-notice">
      <a class="brand" routerLink="/mobile-app">
        <span class="brand-icon"><mat-icon>directions_car</mat-icon></span>
        <span><strong>Conduite</strong><small>ESPACE CANDIDAT</small></span>
      </a>
      <section class="notice-card">
        <span class="notice-icon"><mat-icon>phone_iphone</mat-icon></span>
        <p class="eyebrow">ESPACE CANDIDAT</p>
        <h1>Continuez sur l’application mobile</h1>
        <p>Les services candidat sont disponibles dans l’application mobile. Connectez-vous-y avec le même compte pour consulter votre planning, vos paiements et votre progression.</p>
        <button mat-flat-button type="button" (click)="logout()"><mat-icon>logout</mat-icon>Déconnexion</button>
      </section>
    </main>
  `,
  styles: [`
    :host { display:block; min-height:100vh; color:#20352f; }
    .mobile-notice { min-height:100vh; padding:24px; background:radial-gradient(circle at 50% 35%,#fff 0,#f7f8f5 62%); }
    .brand { display:flex; width:max-content; align-items:center; gap:10px; color:#173d32; text-decoration:none; }
    .brand-icon { display:grid; width:36px; height:36px; place-items:center; border-radius:11px; background:#e7f2eb; color:#26734e; }
    .brand strong,.brand small { display:block; }
    .brand small { margin-top:2px; color:#87918a; font-size:9px; letter-spacing:1.15px; }
    .notice-card { display:flex; max-width:540px; min-height:390px; flex-direction:column; align-items:center; justify-content:center; margin:clamp(48px,12vh,120px) auto 0; padding:42px; border:1px solid #e7ebe5; border-radius:22px; background:#fff; box-shadow:0 18px 55px #173d3210; text-align:center; }
    .notice-icon { display:grid; width:64px; height:64px; place-items:center; border-radius:20px; background:#eaf4ed; color:#32815a; }
    .notice-icon mat-icon { width:32px; height:32px; font-size:32px; }
    .eyebrow { margin:22px 0 8px; color:#39805e; font-size:10px; font-weight:700; letter-spacing:1.2px; }
    h1 { margin:0; color:#1c3b31; font-size:clamp(25px,5vw,34px); }
    .notice-card > p:not(.eyebrow) { max-width:420px; margin:14px 0 24px; color:#718078; font-size:14px; line-height:1.7; }
    .notice-card button { min-height:42px; border-radius:10px; background:#26734e; color:#fff; }
    @media(max-width:520px) { .mobile-notice{padding:18px}.notice-card{min-height:360px;margin-top:8vh;padding:28px 22px} }
  `],
})
export class MobileAppComponent {
  constructor(private readonly authService: AuthService, private readonly router: Router) {}

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
