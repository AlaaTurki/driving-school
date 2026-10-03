import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    RouterLink,
  ],
  templateUrl: './register.component.html',
  styleUrl: '../login/login.component.scss',
})
export class RegisterComponent {
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly form;
  readonly confirmPasswordErrorMatcher: ErrorStateMatcher = {
    isErrorState: (control, form) =>
      !!control &&
      ((control.invalid && (control.touched || !!form?.submitted)) ||
        (control.touched && !!control.parent?.hasError('passwordMismatch'))),
  };

  constructor(
    formBuilder: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.form = formBuilder.nonNullable.group(
      {
        fullName: ['', [Validators.required, Validators.maxLength(120)]],
        email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
        phone: ['', [Validators.required, Validators.pattern(/^(?=(?:\D*\d){7})[+()0-9. -]{7,30}$/)]],
        password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
        confirmPassword: ['', Validators.required],
      },
      { validators: (control) => this.passwordsMatch(control) },
    );
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);
    const { fullName, email, phone, password } = this.form.getRawValue();
    this.authService.register({ fullName, email, phone, password })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/dashboard'),
        error: (error: unknown) => this.errorMessage.set(this.getErrorMessage(error)),
      });
  }

  private passwordsMatch(control: AbstractControl): ValidationErrors | null {
    return control.get('password')?.value === control.get('confirmPassword')?.value
      ? null
      : { passwordMismatch: true };
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que l’API est démarrée.';
    }

    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Cette adresse e-mail possède déjà un compte.';
    }

    return 'La création du compte a échoué. Veuillez réessayer.';
  }
}
