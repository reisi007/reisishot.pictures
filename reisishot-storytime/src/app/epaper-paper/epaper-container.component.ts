import {AfterContentInit, Component, Input, Optional} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {Subject} from "rxjs";
import {EPaperPageDirective} from "./epaper-page/e-paper-page.directive";

@Component({
  selector: 'e-paper-container',
  templateUrl: './epaper-container.component.html',
  styleUrls: ['./epaper-container.component.scss']
})
export class EPaperContainer implements AfterContentInit {

  public currentPageNumber: number = 1

  get currentPageItem(): EPaperPageDirective {
    return this.pages[this.currentPageNumber - 1]
  }

  private currentPageSubject = new Subject<number>()
  public currentPageChange = this.currentPageSubject.asObservable()
  public pages: Array<EPaperPageDirective> = [];

  @Input("previewMode")
  @Optional()
  private isPreview: boolean | null

  @Input()
  public magazinTitle: string

  @Optional()
  @Input()
  public kioskName: string | null

  constructor(
    private router: Router,
    public route: ActivatedRoute,
  ) {

  }

  ngAfterContentInit(): void {
    let prevPageIndex = -1;
    this.currentPageChange.subscribe(pageNumber => {
      const pageIndex = pageNumber - 1
      if (prevPageIndex >= 0)
        this.pages[prevPageIndex].showPage(false)
      this.pages[pageIndex].showPage(true)
      prevPageIndex = pageIndex;
    });
    this.route.paramMap.subscribe(params => {
      const pageNumber = params.get('page');
      this.currentPageNumber = +pageNumber
      this.currentPageSubject.next(this.currentPageNumber)
    })
  }

  onSelectPageChanged(targetPageNumber: number) {
    this.router.navigate(['..', targetPageNumber],
      {relativeTo: this.route})
  }
}
