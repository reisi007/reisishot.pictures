import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {ReisishotSampleMagazineComponent} from './reisishot-sample-magazine.component';

describe('ReisishotSampleMagazineComponent', () => {
  let component: ReisishotSampleMagazineComponent;
  let fixture: ComponentFixture<ReisishotSampleMagazineComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
             declarations: [ReisishotSampleMagazineComponent],
           })
           .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ReisishotSampleMagazineComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
