import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { Candidate } from '../../core/candidates/candidate.models';
import { CandidateService } from '../../core/candidates/candidate.service';
import { CandidateSkills, CandidateSkill } from '../../core/skills/skill.models';
import { SkillsService } from '../../core/skills/skills.service';

@Component({
  selector: 'app-progression',
  standalone: true,
  imports: [DecimalPipe, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './progression.component.html',
  styleUrl: './progression.component.scss',
})
export class ProgressionComponent {
  readonly candidateId: string | null;
  readonly isCandidate: boolean;
  readonly canEdit: boolean;
  readonly progress = signal<CandidateSkills | null>(null);
  readonly candidate = signal<Candidate | null>(null);
  readonly draftLevels = signal<Record<string, number>>({});
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly previewProgress = computed(() => {
    const skills = this.progress()?.skills ?? [];
    if (!skills.length) return 0;
    const total = skills.reduce((sum, skill) => sum + (this.draftLevels()[skill.skillId] ?? skill.level), 0);
    return Math.round(total / skills.length);
  });

  constructor(
    private readonly authService: AuthService,
    private readonly skillsService: SkillsService,
    private readonly candidateService: CandidateService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {
    this.candidateId = this.route.snapshot.paramMap.get('candidateId');
    const roles = this.authService.getSession()?.roles ?? [];
    this.isCandidate = this.candidateId === null;
    this.canEdit = this.candidateId !== null && (roles.includes('ADMIN') || roles.includes('INSTRUCTOR'));
    this.load();
  }

  load(): void {
    this.errorMessage.set('');
    this.loading.set(true);
    if (this.candidateId) {
      forkJoin({
          progress: this.skillsService.getCandidateSkills(this.candidateId),
          candidate: this.candidateService.findById(this.candidateId),
        }).pipe(finalize(() => this.loading.set(false))).subscribe({
          next: (result) => {
            this.applyProgress(result.progress);
            this.candidate.set(result.candidate);
          },
          error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
        });
      return;
    }

    this.skillsService.getMySkills().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (progress) => this.applyProgress(progress),
      error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
    });
  }

  setLevel(skillId: string, event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLInputElement)) return;
    this.draftLevels.update((levels) => ({ ...levels, [skillId]: Number(target.value) }));
    this.successMessage.set('');
  }

  level(skill: CandidateSkill): number {
    return this.draftLevels()[skill.skillId] ?? skill.level;
  }

  save(): void {
    const candidateId = this.candidateId;
    const progress = this.progress();
    if (!candidateId || !progress || !this.canEdit || this.saving()) return;

    this.saving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.skillsService.updateCandidateSkills(candidateId, {
      skills: progress.skills.map((skill) => ({
        skillId: skill.skillId,
        level: this.level(skill),
      })),
    }).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (updated) => {
        this.progress.set(updated);
        this.draftLevels.set(Object.fromEntries(
          updated.skills.map((skill) => [skill.skillId, skill.level]),
        ));
        this.successMessage.set('Progression enregistrée.');
      },
      error: (error: unknown) => this.errorMessage.set(this.errorText(error)),
    });
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private applyProgress(progress: CandidateSkills): void {
    this.progress.set(progress);
    this.draftLevels.set(Object.fromEntries(
      progress.skills.map((skill) => [skill.skillId, skill.level]),
    ));
  }

  private errorText(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires pour consulter cette progression.';
    }
    if (error instanceof HttpErrorResponse && error.status === 404) {
      return 'Candidat ou compétence introuvable.';
    }
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return 'Chaque niveau doit être compris entre 0 et 100.';
    }
    return 'La progression n’a pas pu être chargée. Veuillez réessayer.';
  }
}
