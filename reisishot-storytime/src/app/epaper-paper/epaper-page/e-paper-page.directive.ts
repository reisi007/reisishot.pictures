import {Directive, Input, TemplateRef, ViewContainerRef} from '@angular/core';
import {EPaperContainer} from "../epaper-container.component";
import {NgIf} from "@angular/common";

@Directive({
  selector: '[ePaperPage]'
})
export class EPaperPageDirective {

  private _showPage: NgIf<boolean>;

  @Input('ePaperPage')
  articleTitle: string

  constructor(container: EPaperContainer, viewContainer: ViewContainerRef, templateRef: TemplateRef<any>) {
    container.pages.push(this);
    this._showPage = new NgIf<boolean>(viewContainer, templateRef)
    this._showPage.ngIf = false;
  }

  showPage(isShown: boolean) {
    this._showPage.ngIf = isShown;
  }
}
