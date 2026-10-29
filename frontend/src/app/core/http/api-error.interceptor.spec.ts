import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorInterceptor } from './api-error.interceptor';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let snackbar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(() => {
    snackbar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackbar },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('does not show a snackbar for an absolute driving-session 409 URL', () => {
    const url = 'http://localhost:4200/api/driving-sessions/93998abd-4601-4b7d-836a-1d55f20d49c3/status';
    http.patch(url, {}).subscribe({ error: () => undefined });

    httpTesting.expectOne(url).flush(
      { message: 'Conflict' },
      { status: 409, statusText: 'Conflict' },
    );

    expect(snackbar.open).not.toHaveBeenCalled();
  });
});
