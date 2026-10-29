import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabChangeEvent, MatTabsModule } from '@angular/material/tabs';
import { BalanceSummary, Payment, StatementPage } from '../../core/financials/financial.models';
import { FinancialService } from '../../core/financials/financial.service';
import { CandidateSkills } from '../../core/skills/skill.models';
import { SkillsService } from '../../core/skills/skills.service';
import { DtCurrencyPipe } from '../../shared/pipes/dt-currency.pipe';
import { Candidate } from '../../core/candidates/candidate.models';
import { CandidateService } from '../../core/candidates/candidate.service';
import { LocalDatePipe } from '../../shared/pipes/local-date.pipe';
import { LocalTimePipe } from '../../shared/pipes/local-time.pipe';

@Component({
  selector: 'app-candidate-detail',
  standalone: true,
  imports: [RouterLink, LocalDatePipe, LocalTimePipe, DtCurrencyPipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTabsModule],
  template: `
    <main class="detail-page">
      <section class="page-content">
        <a class="back-link" routerLink="/candidates"><mat-icon>arrow_back</mat-icon>Retour aux candidats</a>
        @if (errorMessage()) { <p class="notice error" role="alert">{{ errorMessage() }}</p> }
        @if (loading()) {
          <div class="loading-state"><mat-spinner diameter="32" /><span>Chargement du profil...</span></div>
        } @else if (candidate(); as learner) {
          <header class="profile-heading">
            <span class="avatar">{{ learner.firstName.charAt(0) }}{{ learner.lastName.charAt(0) }}</span>
            <div><p class="eyebrow">PROFIL CANDIDAT</p><h1>{{ learner.firstName }} {{ learner.lastName }}</h1><p>{{ learner.email }}</p></div>
            <a mat-stroked-button [routerLink]="['/candidates', learner.id, 'skills']"><mat-icon>trending_up</mat-icon>Progression</a>
          </header>
          @if (balance(); as totals) {
            <section class="balance-strip" aria-label="Résumé financier du candidat">
              <article><small>Total séances</small><strong>{{ totals.totalSessions | dtCurrency }}</strong></article>
              <article><small>Total payé</small><strong>{{ totals.totalPaid | dtCurrency }}</strong></article>
              <article [class.credit]="totals.credit"><small>{{ totals.credit ? 'Avoir' : 'Reste à payer' }}</small>
                <strong>{{ (totals.remaining < 0 ? -totals.remaining : totals.remaining) | dtCurrency }}</strong></article>
            </section>
          }
          <mat-tab-group animationDuration="150ms" (selectedTabChange)="onTabChange($event)">
            <mat-tab label="Informations">
              <section class="details-grid">
                <article><small>Téléphone</small><strong>{{ learner.phone }}</strong></article>
                <article><small>Date de naissance</small><strong>{{ learner.dateOfBirth | localDate }}</strong></article>
                <article><small>Adresse</small><strong>{{ learner.address || '—' }}</strong></article>
                <article><small>Inscrit le</small><strong>{{ learner.registrationDate | localDate }}</strong></article>
                <article><small>Statut</small><strong>{{ statusLabel(learner.status) }}</strong></article>
                <article><small>Notes</small><strong>{{ learner.notes || '—' }}</strong></article>
              </section>
            </mat-tab>
            <mat-tab label="Séances">
              @if (loadingTab() === 'sessions') { <div class="loading-state"><mat-spinner diameter="24" />Chargement des séances…</div> }
              @if (sessionStatement(); as statement) {
                <div class="tab-toolbar"><span>{{ statement.totalElements }} séance(s)</span><a mat-stroked-button [routerLink]="['/sessions']" [queryParams]="{ candidateId: learner.id }">Ouvrir les séances</a></div>
                @if (statement.content.length) {
                  <div class="table-wrap"><table><thead><tr><th>Jour</th><th>Date</th><th>Heure</th><th>Heures</th><th>Statut</th><th>Prix</th><th>Payé</th><th>Reste à payer</th></tr></thead>
                    <tbody>@for (line of statement.content; track line.sessionId) {
                      <tr><td>{{ line.day }}</td><td>{{ line.date | localDate }}</td><td>{{ line.startTime | localTime }}–{{ line.endTime | localTime }}</td>
                        <td>{{ line.hours }} h</td><td>{{ sessionStatusLabel(line.status) }}</td><td>{{ line.price | dtCurrency }}</td><td>{{ line.paidAmount | dtCurrency }}</td><td>{{ line.price - line.paidAmount | dtCurrency }}</td></tr>
                    }</tbody>
                  </table></div>
                } @else { <section class="placeholder"><mat-icon>event_busy</mat-icon><strong>Aucune séance enregistrée.</strong></section> }
              }
            </mat-tab>
            <mat-tab label="Paiements">
              @if (loadingTab() === 'payments') { <div class="loading-state"><mat-spinner diameter="24" />Chargement des paiements…</div> }
              <div class="tab-toolbar"><span>Historique des règlements</span>
                <a mat-stroked-button [routerLink]="['/payments']" [queryParams]="{ candidateId: learner.id }">Ouvrir les paiements</a>
              </div>
              @if (payments().length) {
                <div class="table-wrap"><table><thead><tr><th>Date</th><th>Montant</th><th>Moyen de paiement</th><th>Note</th></tr></thead>
                  <tbody>@for (payment of payments(); track payment.id) {
                    <tr><td>{{ payment.paymentDate | localDate }}</td><td>{{ payment.amount | dtCurrency }}</td><td>{{ paymentMethodLabel(payment.paymentMethod) }}</td><td>{{ payment.notes || '—' }}</td></tr>
                  }</tbody>
                </table></div>
              } @else if (paymentsLoaded()) { <section class="placeholder"><mat-icon>receipt_long</mat-icon><strong>Aucun paiement enregistré.</strong></section> }
            </mat-tab>
            <mat-tab label="Progression">
              @if (loadingTab() === 'skills') { <div class="loading-state"><mat-spinner diameter="24" />Chargement de la progression…</div> }
              @if (progress(); as result) {
                <div class="progress-summary"><span>Progression globale</span><strong>{{ result.overallProgress }}%</strong></div>
                <div class="skill-list">@for (skill of result.skills; track skill.skillId) {
                  <article><div><strong>{{ skill.name }}</strong><span>{{ skillLevel(skill.skillId, skill.level) }}%</span></div>
                    <input type="range" min="0" max="100" step="1" [value]="skillLevel(skill.skillId, skill.level)"
                      (input)="setSkillLevel(skill.skillId, $event)" [disabled]="!canEditSkills || savingSkills()" [attr.aria-label]="skill.name" />
                    <progress [value]="skillLevel(skill.skillId, skill.level)" max="100">{{ skillLevel(skill.skillId, skill.level) }}%</progress></article>
                }</div>
                <div class="tab-toolbar"><span>Progression globale modifiée : {{ previewProgress() }}%</span>
                  @if (canEditSkills) {
                    <button mat-flat-button color="primary" type="button" (click)="saveSkills()" [disabled]="savingSkills()">
                      @if (savingSkills()) { <mat-spinner diameter="18" /> } @else { Enregistrer les niveaux }
                    </button>
                  }
                </div>
                @if (skillsSuccess()) { <p class="notice success" role="status">{{ skillsSuccess() }}</p> }
              }
            </mat-tab>
          </mat-tab-group>
          @if (tabError()) { <p class="notice error" role="alert">{{ tabError() }}</p> }
        }
      </section>
    </main>
  `,
  styles: [`
    :host { display:block; min-height:100vh; color:#20352f; }
    .detail-page { min-height:100vh; background:#f7f8f5; }
    .page-content { width:min(100% - 48px,1160px); margin:0 auto; padding:34px 0 60px; }
    .back-link { display:inline-flex; align-items:center; gap:7px; margin-bottom:22px; color:#567363; font-size:12px; text-decoration:none; }
    .back-link mat-icon { width:17px; height:17px; font-size:17px; }
    .profile-heading { display:flex; align-items:center; gap:14px; margin-bottom:24px; padding:22px; border:1px solid #e8ece6; border-radius:14px; background:#fff; }
    .avatar { display:grid; width:52px; height:52px; flex:0 0 52px; place-items:center; border-radius:50%; background:#e8f1e8; color:#216347; font-weight:700; }
    .profile-heading > div { flex:1; min-width:0; }
    .eyebrow { margin:0 0 5px; color:#718177; font-size:9px; font-weight:750; letter-spacing:.15em; }
    h1 { margin:0; color:#293e33; font:400 clamp(24px,4vw,34px) Georgia,'Times New Roman',serif; }
    .profile-heading p:last-child { margin:5px 0 0; color:#829087; font-size:12px; }
    .details-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px; padding:20px; }
    .details-grid article { display:grid; gap:7px; min-height:70px; padding:14px; border:1px solid #e9ece6; border-radius:10px; background:#fff; }
    .details-grid small { color:#87938a; font-size:10px; }
    .details-grid strong { color:#405349; font-size:12px; font-weight:600; overflow-wrap:anywhere; }
    .balance-strip { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:10px; margin:0 0 14px; }
    .balance-strip article { display:grid; gap:7px; padding:14px; border:1px solid #e8ece6; border-radius:10px; background:#fff; }
    .balance-strip small { color:#87938a; font-size:10px; }
    .balance-strip strong { color:#34483c; font-size:15px; }
    .balance-strip .credit strong { color:#34724e; }
    .tab-toolbar { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:14px 18px; color:#718177; font-size:11px; }
    .table-wrap { overflow:auto; }
    table { width:100%; border-collapse:collapse; text-align:left; }
    th { padding:11px 12px; color:#89958c; font-size:9px; text-transform:uppercase; white-space:nowrap; }
    td { padding:11px 12px; border-top:1px solid #f0f2ee; color:#64746a; font-size:10px; white-space:nowrap; }
    .progress-summary { display:flex; justify-content:space-between; align-items:center; padding:16px 18px; color:#53665a; font-size:12px; }
    .progress-summary strong { color:#26734c; font-size:20px; }
    .skill-list { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; padding:0 18px 16px; }
    .skill-list article { display:grid; gap:9px; padding:12px; border:1px solid #e9ece6; border-radius:9px; }
    .skill-list article div { display:flex; justify-content:space-between; color:#53665a; font-size:11px; }
    .skill-list article span { color:#26734c; font-weight:700; }
    .skill-list input[type=range] { width:100%; accent-color:#39845a; }
    .skill-list progress { width:100%; height:7px; accent-color:#39845a; }
    .placeholder,.loading-state { display:flex; min-height:220px; align-items:center; justify-content:center; flex-direction:column; gap:9px; color:#829087; text-align:center; }
    .placeholder mat-icon { color:#7a9a82; }
    .placeholder strong { color:#506358; font-size:13px; }
    .placeholder span { font-size:11px; }
    .notice { padding:11px 14px; border:1px solid #f0c9c3; border-radius:8px; background:#fff4f2; color:#a43e33; font-size:12px; }
    .notice.success { border-color:#d5e7d6; background:#f1f8f0; color:#36714a; }
    @media(max-width:700px) { .page-content{width:calc(100% - 32px);padding-top:24px}.profile-heading{align-items:flex-start;flex-wrap:wrap}.profile-heading > div{flex-basis:calc(100% - 70px)}.details-grid{grid-template-columns:1fr 1fr;padding:12px}.balance-strip{grid-template-columns:1fr}.skill-list{grid-template-columns:1fr} }
    @media(max-width:430px) { .details-grid{grid-template-columns:1fr} }
  `],
})
export class CandidateDetailComponent {
  readonly candidate = signal<Candidate | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly tabError = signal('');
  readonly loadingTab = signal('');
  readonly balance = signal<BalanceSummary | null>(null);
  readonly sessionStatement = signal<StatementPage | null>(null);
  readonly payments = signal<Payment[]>([]);
  readonly paymentsLoaded = signal(false);
  readonly progress = signal<CandidateSkills | null>(null);
  private readonly loadedTabs = new Set<number>();
  readonly skillDrafts = signal<Record<string, number>>({});
  readonly savingSkills = signal(false);
  readonly skillsSuccess = signal('');
  readonly canEditSkills = true;
  readonly previewProgress = computed(() => {
    const skills = this.progress()?.skills ?? [];
    if (!skills.length) return 0;
    return Math.round(skills.reduce((sum, skill) => sum + this.skillLevel(skill.skillId, skill.level), 0) / skills.length);
  });

  constructor(
    route: ActivatedRoute,
    candidates: CandidateService,
    private readonly financialService: FinancialService,
    private readonly skillsService: SkillsService,
  ) {
    route.paramMap.subscribe((params) => {
      const id = params.get('candidateId');
      if (!id) {
        this.errorMessage.set('Identifiant candidat manquant.');
        this.loading.set(false);
        return;
      }
      this.loadedTabs.clear();
      this.tabError.set('');
      this.balance.set(null);
      this.sessionStatement.set(null);
      this.payments.set([]);
      this.paymentsLoaded.set(false);
      this.progress.set(null);
      this.skillDrafts.set({});
      this.candidate.set(null);
      this.errorMessage.set('');
      this.loading.set(true);
      candidates.findById(id).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: (candidate) => this.candidate.set(candidate),
        error: (error: unknown) => this.errorMessage.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Vous ne disposez pas des droits nécessaires pour consulter ce candidat.'
          : 'Le profil candidat n’a pas pu être chargé.'),
      });
    });
  }

  onTabChange(event: MatTabChangeEvent): void {
    const id = this.candidate()?.id;
    if (!id || event.index === 0 || this.loadedTabs.has(event.index)) return;
    this.loadedTabs.add(event.index);
    this.tabError.set('');
    if (event.index === 1) {
      this.loadingTab.set('sessions');
      this.financialService.getStatement({ candidateId: id, page: 0, size: 100 })
        .pipe(finalize(() => this.loadingTab.set('')))
        .subscribe({
          next: (statement) => this.sessionStatement.set(statement),
          error: () => { this.loadedTabs.delete(1); this.tabError.set('Le relevé des séances est indisponible.'); },
        });
    } else if (event.index === 2) {
      this.loadingTab.set('payments');
      forkJoin({
        balance: this.financialService.getBalance(id),
        payments: this.financialService.getPayments(id, 0, 100),
      }).pipe(finalize(() => this.loadingTab.set('')))
        .subscribe({
          next: (result) => { this.balance.set(result.balance); this.payments.set(result.payments.content); this.paymentsLoaded.set(true); },
          error: () => { this.loadedTabs.delete(2); this.tabError.set('Les paiements ou le solde de ce candidat sont indisponibles.'); },
        });
    } else if (event.index === 3) {
      this.loadingTab.set('skills');
      this.skillsService.getCandidateSkills(id).pipe(finalize(() => this.loadingTab.set('')))
        .subscribe({
          next: (progress) => this.progress.set(progress),
          error: () => { this.loadedTabs.delete(3); this.tabError.set('La progression de ce candidat est indisponible.'); },
        });
    }
  }

  sessionStatusLabel(status: StatementPage['content'][number]['status']): string {
    return { PLANNED: 'Planifiée', DONE: 'Effectuée', CANCELLED: 'Annulée', ABSENT: 'Absent' }[status];
  }

  paymentMethodLabel(method: Payment['paymentMethod']): string {
    return { CASH: 'Espèces', CARD: 'Carte', BANK_TRANSFER: 'Virement', OTHER: 'Autre' }[method];
  }

  skillLevel(skillId: string, original: number): number {
    return this.skillDrafts()[skillId] ?? original;
  }

  setSkillLevel(skillId: string, event: Event): void {
    const target = event.target;
    if (target instanceof HTMLInputElement) {
      this.skillDrafts.update((levels) => ({ ...levels, [skillId]: Number(target.value) }));
      this.skillsSuccess.set('');
    }
  }

  saveSkills(): void {
    const id = this.candidate()?.id;
    const progress = this.progress();
    if (!id || !progress || this.savingSkills()) return;
    this.savingSkills.set(true);
    this.tabError.set('');
    this.skillsSuccess.set('');
    this.skillsService.updateCandidateSkills(id, {
      skills: progress.skills.map((skill) => ({ skillId: skill.skillId, level: this.skillLevel(skill.skillId, skill.level) })),
    }).pipe(finalize(() => this.savingSkills.set(false))).subscribe({
      next: (updated) => {
        this.progress.set(updated);
        this.skillDrafts.set({});
        this.skillsSuccess.set('Progression enregistrée.');
      },
      error: (error: unknown) => this.tabError.set(error instanceof HttpErrorResponse && error.status === 400
        ? 'Chaque niveau doit être compris entre 0 et 100.'
        : 'La progression n’a pas pu être enregistrée. Vérifiez vos droits et réessayez.'),
    });
  }

  statusLabel(status: Candidate['status']): string {
    return ({ ACTIVE: 'Actif', INACTIVE: 'Inactif', COMPLETED: 'Terminé', SUSPENDED: 'Suspendu' })[status];
  }
}
