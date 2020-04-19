import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-reisishot-sample-magazine',
  templateUrl: './reisishot-sample-magazine.component.html',
  styleUrls: ['./reisishot-sample-magazine.component.scss'],
})
export class ReisishotSampleMagazineComponent implements OnInit {

  constructor() {
  }

  ngOnInit(): void {
  }

  getRoutePart(): string {
    return 'sample';
  }

}
