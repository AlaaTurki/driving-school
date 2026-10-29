import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
}

@Component({
  selector: 'app-paged-table',
  standalone: true,
  imports: [MatTableModule, MatPaginatorModule, MatSortModule],
  template: `
    <div class="table-scroll">
      <table mat-table [dataSource]="rows" matSort (matSortChange)="sortChange.emit($event)">
        @for (column of columns; track column.key) {
          <ng-container [matColumnDef]="column.key">
            <th mat-header-cell *matHeaderCellDef [mat-sort-header]="column.sortable ? column.key : ''"
                [disabled]="!column.sortable">{{ column.label }}</th>
            <td mat-cell *matCellDef="let row">{{ cellValue(row, column.key) }}</td>
          </ng-container>
        }
        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
        <tr class="mat-row" *matNoDataRow><td class="mat-cell" [attr.colspan]="columns.length">Aucun résultat.</td></tr>
      </table>
    </div>
    <mat-paginator [length]="totalItems" [pageIndex]="pageIndex" [pageSize]="pageSize"
                   [pageSizeOptions]="pageSizeOptions" (page)="pageChange.emit($event)"
                   aria-label="Pagination des résultats" />
  `,
  styles: [`
    :host { display:block; min-width:0; overflow:hidden; border:1px solid #e7ebe5; border-radius:12px; background:#fff; }
    .table-scroll { overflow-x:auto; }
    table { width:100%; }
    th { color:#718078; font-size:11px; font-weight:650; white-space:nowrap; }
    td { color:#405349; font-size:12px; }
    mat-paginator { border-top:1px solid #edf0ec; }
  `],
})
export class PagedTableComponent<T extends object> {
  @Input() columns: TableColumn[] = [];
  @Input() rows: T[] = [];
  @Input() totalItems = 0;
  @Input() pageIndex = 0;
  @Input() pageSize = 20;
  @Input() pageSizeOptions = [10, 20, 50];
  @Output() readonly pageChange = new EventEmitter<PageEvent>();
  @Output() readonly sortChange = new EventEmitter<Sort>();

  get displayedColumns(): string[] {
    return this.columns.map((column) => column.key);
  }

  cellValue(row: T, key: string): unknown {
    return Reflect.get(row, key) ?? '—';
  }
}
