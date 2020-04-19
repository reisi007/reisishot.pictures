import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {EPaperContainer} from './epaper-container.component';

describe('EpaperPaperComponent', () => {
  let component: EPaperContainer;
  let fixture: ComponentFixture<EPaperContainer>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
             declarations: [EPaperContainer],
           })
           .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EPaperContainer);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
