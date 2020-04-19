import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {SampleMagazineSampleArticleComponent} from './sample-magazine-sample-article.component';

describe('SampleMagazineSampleArticleComponent', () => {
  let component: SampleMagazineSampleArticleComponent;
  let fixture: ComponentFixture<SampleMagazineSampleArticleComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
             declarations: [SampleMagazineSampleArticleComponent],
           })
           .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleMagazineSampleArticleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
