import { DatePipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'localDate', standalone: true })
export class LocalDatePipe implements PipeTransform {
  private readonly datePipe = new DatePipe('fr');

  transform(value: string | Date | null | undefined): string {
    if (!value) return '—';
    if (typeof value === 'string') {
      const dateOnly = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
      if (dateOnly) return `${dateOnly[3]}/${dateOnly[2]}/${dateOnly[1]}`;
    }
    return this.datePipe.transform(value, 'dd/MM/yyyy', 'UTC') ?? '—';
  }
}
