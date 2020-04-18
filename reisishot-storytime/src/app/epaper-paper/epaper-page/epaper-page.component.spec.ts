import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {EpaperPageComponent} from './epaper-page.component';

describe('EpaperPageComponent', () => {
  let component: EpaperPageComponent;
  let fixture: ComponentFixture<EpaperPageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [EpaperPageComponent]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EpaperPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
