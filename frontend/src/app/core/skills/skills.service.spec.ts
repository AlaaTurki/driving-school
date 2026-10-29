import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SkillsService } from './skills.service';

describe('SkillsService', () => {
  let service: SkillsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SkillsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the candidate skills for an instructor or administrator', () => {
    service.getCandidateSkills('candidate-id').subscribe();
    expect(http.expectOne('/api/candidates/candidate-id/skills').request.method).toBe('GET');
  });

  it('loads the current candidate skills without sending a candidate id', () => {
    service.getMySkills().subscribe();
    expect(http.expectOne('/api/me/skills').request.method).toBe('GET');
  });

  it('saves several skill levels in one request', () => {
    const request = { skills: [{ skillId: 'skill-id', level: 75 }] };
    service.updateCandidateSkills('candidate-id', request).subscribe();
    const testRequest = http.expectOne('/api/candidates/candidate-id/skills');
    expect(testRequest.request.method).toBe('PUT');
    expect(testRequest.request.body).toEqual(request);
  });
});
