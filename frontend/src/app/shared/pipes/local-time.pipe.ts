import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'localTime', standalone: true })
export class LocalTimePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '—';
    const match = /^([01]\d|2[0-3]):([0-5]\d)(?::[0-5]\d)?$/.exec(value);
    return match ? `${match[1]}:${match[2]}` : '—';
  }
}
