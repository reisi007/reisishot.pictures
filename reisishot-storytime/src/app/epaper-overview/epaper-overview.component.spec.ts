import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {EpaperOverviewComponent} from './epaper-overview.component';

describe('EpaperOverviewComponent', () => {
  let component: EpaperOverviewComponent;
  let fixture: ComponentFixture<EpaperOverviewComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [EpaperOverviewComponent]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EpaperOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
