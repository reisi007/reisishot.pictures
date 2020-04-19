import {Injectable} from '@angular/core';
import {EPaperContainer} from './epaper-container.component';

@Injectable({
  providedIn: 'root',
})
export class EpaperKioskService {

  private epapers = new Map<string, Array<EPaperContainer>>();

  getEPapers(kiosk: string) {
    return this.epapers.get(kiosk);
  }

  putEPaper(kiosk: string, epaper: EPaperContainer) {
    let ePaperContainers = this.epapers.get(kiosk);
    if(ePaperContainers === undefined) {
      ePaperContainers = new Array<EPaperContainer>();
      this.epapers.set(kiosk, ePaperContainers);
    }
    ePaperContainers.push(epaper);
  }

  removeEpaper(kiosk: string) {
    this.epapers.delete(kiosk);
  }

  constructor() {
  }
}
