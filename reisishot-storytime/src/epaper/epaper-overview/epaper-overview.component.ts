import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'epaper-overview',
  templateUrl: './epaper-overview.component.html',
  styleUrls: ['./epaper-overview.component.scss'],
})
export class EpaperOverviewComponent implements OnInit {

  constructor(
    private router: Router,
    public route: ActivatedRoute,
  ) {

  }

  ngOnInit(): void {
  }


}

