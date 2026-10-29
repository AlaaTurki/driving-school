import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { InstructorService } from '../../core/instructors/instructor.service';
import { ConfirmationDialogService } from '../../shared/components/confirm-dialog.component';
import { InstructorsComponent } from './instructors.component';

describe('InstructorsComponent form', () => {
  let instructors: jasmine.SpyObj<InstructorService>;

  beforeEach(() => {
    instructors = jasmine.createSpyObj<InstructorService>('InstructorService', ['findAll', 'create', 'update', 'delete']);
    instructors.findAll.and.returnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true,
    }));
    instructors.create.and.returnValue(of({
      id: 'instructor-id', userId: 'user-id', firstName: 'Grace', lastName: 'Hopper',
      phone: '+21612345678', email: 'grace@example.com', licenseNumber: 'LIC-1',
      status: 'ACTIVE', createdAt: '', updatedAt: '',
    }));
    TestBed.configureTestingModule({
      imports: [InstructorsComponent],
      providers: [
        { provide: InstructorService, useValue: instructors },
        { provide: ConfirmationDialogService, useValue: { confirm: () => of(false) } },
      ],
    });
  });

  it('requires a valid initial password before creating an instructor', () => {
    const component = TestBed.createComponent(InstructorsComponent).componentInstance;
    component.startCreate();
    component.form.patchValue({ firstName: 'Grace', lastName: 'Hopper', phone: '+21612345678', email: 'grace@example.com', licenseNumber: 'LIC-1' });
    component.save();

    expect(component.form.invalid).toBeTrue();
    expect(instructors.create).not.toHaveBeenCalled();
  });

  it('submits a valid instructor account and clears the password control immediately', () => {
    const component = TestBed.createComponent(InstructorsComponent).componentInstance;
    component.startCreate();
    component.form.patchValue({
      firstName: 'Grace', lastName: 'Hopper', phone: '+21612345678',
      email: 'grace@example.com', licenseNumber: 'LIC-1', initialPassword: 'a-secure-password',
    });
    component.save();

    expect(instructors.create).toHaveBeenCalledWith(jasmine.objectContaining({
      email: 'grace@example.com',
      initialPassword: 'a-secure-password',
    }));
    expect(component.form.controls['initialPassword'].value).toBe('');
  });
});
