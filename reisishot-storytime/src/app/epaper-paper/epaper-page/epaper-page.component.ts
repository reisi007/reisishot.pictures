import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {EPaperContainer} from "../epaper-container.component";

@Component({
  selector: 'epaper-page',
  templateUrl: './epaper-page.component.html',
  styleUrls: ['./epaper-page.component.scss']
})
export class EpaperPageComponent implements OnInit {

  public showPage: boolean = false;

  constructor(public cd: ChangeDetectorRef, private container: EPaperContainer) {
    container.pages.push(this);
  }

  ngOnInit(): void {
  }

}
