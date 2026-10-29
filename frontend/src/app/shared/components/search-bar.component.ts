import { Component, DestroyRef, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [ReactiveFormsModule, MatIconModule],
  template: `
    <label class="search-bar">
      <mat-icon>search</mat-icon>
      <span class="visually-hidden">Rechercher</span>
      <input type="search" [placeholder]="placeholder" [formControl]="control" />
    </label>
  `,
  styles: [`
    :host { display:block; width:min(100%, 360px); min-width:220px; }
    .search-bar { box-sizing:border-box; display:flex; width:100%; min-height:38px; align-items:center; gap:9px; padding:0 12px; border:1px solid #e4e9e3; border-radius:10px; background:#fff; color:#849087; }
    .search-bar mat-icon { width:18px; height:18px; font-size:18px; }
    input { width:100%; min-height:40px; border:0; outline:0; background:transparent; color:#394b41; font:inherit; font-size:12px; }
    .visually-hidden { position:absolute; width:1px; height:1px; overflow:hidden; clip:rect(0,0,0,0); white-space:nowrap; clip-path:inset(50%); }
  `],
})
export class SearchBarComponent {
  private readonly destroyRef = inject(DestroyRef);
  @Output() readonly searchChange = new EventEmitter<string>();
  readonly control = new FormControl('', { nonNullable: true });

  @Input() placeholder = 'Rechercher...';

  constructor() {
    this.control.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.searchChange.emit(value.trim()));
  }
}
