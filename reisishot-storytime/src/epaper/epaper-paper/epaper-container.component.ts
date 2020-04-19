import {AfterContentInit, Component, ElementRef, Input, OnDestroy, OnInit, Optional, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subject} from 'rxjs';
import {EPaperPageDirective} from './epaper-page/e-paper-page.directive';
import {EpaperKioskService} from './epaper-kiosk.service';

@Component({
  selector: 'e-paper-container',
  templateUrl: './epaper-container.component.html',
  styleUrls: ['./epaper-container.component.scss'],
})
export class EPaperContainer implements OnInit, OnDestroy, AfterContentInit {

  public currentPageNumber: number = 1;

  get currentPageItem(): EPaperPageDirective {
    return this.pages[this.currentPageNumber - 1];
  }

  private currentPageSubject = new Subject<number>();
  public currentPageChange = this.currentPageSubject.asObservable();
  public pages: Array<EPaperPageDirective> = [];

  @Input('previewMode')
  @Optional()
  private isPreview: boolean | null;

  @Input()
  public magazinTitle: string;

  @Optional()
  @Input()
  public kioskName: string | null;
  private _kioskName: string | undefined;

  prevScrollY = 0;
  @ViewChild('content') contentRef: ElementRef;
  isScrollDown = false;

  constructor(
    private router: Router,
    public route: ActivatedRoute,
    private kiosk: EpaperKioskService,
  ) {
  }

  ngOnInit(): void {
    if(this.kioskName) {
      this.kiosk.putEPaper(this.kioskName, this);
      this._kioskName = this.kioskName;
    }

  }

  ngAfterContentInit(): void {
    let prevPageIndex = -1;
    this.currentPageChange.subscribe(pageNumber => {
      const pageIndex = pageNumber - 1;
      if(prevPageIndex >= 0) {
        this.pages[prevPageIndex].showPage(false);
      }
      this.pages[pageIndex].showPage(true);
      prevPageIndex = pageIndex;
    });
    this.route.paramMap.subscribe(params => {
      const pageNumber = params.get('page');
      this.currentPageNumber = +pageNumber;
      this.currentPageSubject.next(this.currentPageNumber);
    });
  }

  ngOnDestroy(): void {
    if(this._kioskName) {
      this.kiosk.removeEpaper(this.kioskName);
    }
  }

  onSelectPageChanged(targetPageNumber: number) {
    this.router.navigate(['..', targetPageNumber],
      {relativeTo: this.route});
  }

  onScroll() {
    const scrollY = this.contentRef.nativeElement.scrollTop;
    this.isScrollDown = scrollY > this.prevScrollY;
    this.prevScrollY = scrollY;

  }

}
