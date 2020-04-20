import {Component, ContentChildren, Input, QueryList} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {EPaperDirective} from './e-paper.directive';

@Component({
  selector: 'e-paper-overview',
  templateUrl: './epaper-overview.component.html',
  styleUrls: ['./epaper-overview.component.scss'],
})
export class EpaperOverviewComponent {

  @Input()
  public kioskName: string;

  @Input()
  previewMode: boolean;

  @ContentChildren(EPaperDirective)
  contentChildren: QueryList<EPaperDirective>;

  constructor(
    private router: Router,
    public route: ActivatedRoute,
  ) {

  }
}

