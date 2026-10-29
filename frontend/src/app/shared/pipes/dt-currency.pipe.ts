import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'dtCurrency', standalone: true })
export class DtCurrencyPipe implements PipeTransform {
  private readonly formatter = new Intl.NumberFormat('fr-TN', {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  });

  transform(value: number | null | undefined): string {
    return value == null || !Number.isFinite(value) ? '—' : `${this.formatter.format(value)} DT`;
  }
}
