import {AfterContentInit, Component} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Subject} from "rxjs";
import {EpaperPageComponent} from "./epaper-page/epaper-page.component";

@Component({
  selector: 'e-paper-container',
  templateUrl: './epaper-container.component.html',
  styleUrls: ['./epaper-container.component.scss']
})
export class EPaperContainer implements AfterContentInit {

  public currentPage: number
  private currentPageSubject = new Subject<number>()
  public currentPageChange = this.currentPageSubject.asObservable()
  public pages: Array<EpaperPageComponent> = [];

  constructor(public route: ActivatedRoute) {

  }

  ngAfterContentInit(): void {
    this.initPage()
  }

  private initPage() {
    this.currentPageChange.subscribe(pageNumber => {
      const pageIndex = pageNumber - 1
      this.pages.forEach((item, index) => {
          const prev = item.showPage;
          item.showPage = pageIndex === index;
          if (prev !== item.showPage) {
            item.cd.markForCheck()
          }
        }
      )
    });
    this.route.paramMap.subscribe(params => {
      this.currentPage = +params.get('page')
      this.currentPageSubject.next(this.currentPage)
    })
  }


}
