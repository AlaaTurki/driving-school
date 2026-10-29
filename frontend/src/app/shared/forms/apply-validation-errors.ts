import { AbstractControl, FormGroup } from '@angular/forms';

export function applyValidationErrors(
  form: FormGroup,
  validationErrors: unknown,
): boolean {
  if (typeof validationErrors !== 'object' || validationErrors === null || Array.isArray(validationErrors)) {
    return false;
  }

  let applied = false;
  for (const [field, message] of Object.entries(validationErrors)) {
    const control: AbstractControl | null = form.get(field);
    if (!control || typeof message !== 'string') continue;
    control.setErrors({ ...control.errors, server: message });
    control.markAsTouched();
    applied = true;
  }
  return applied;
}
