import {Component, Input} from '@angular/core';

@Component({
  selector: 'reisishot-sample-magazine',
  templateUrl: './reisishot-sample-magazine.component.html',
  styleUrls: ['./reisishot-sample-magazine.component.scss'],
})
export class ReisishotSampleMagazineComponent {

  @Input()
  previewMode: boolean;

  constructor() {
  }

}
