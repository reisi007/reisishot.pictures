import {TestBed} from '@angular/core/testing';

import {EpaperKioskService} from './epaper-kiosk.service';

describe('EpaperKioskServiceService', () => {
  let service: EpaperKioskService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EpaperKioskService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
