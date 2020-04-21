import {Directive, Input, TemplateRef} from '@angular/core';

@Directive({
  selector: '[ePaper]',
})
export class EPaperDirective {

  @Input('ePaper')
  public route: string;

  constructor(public template: TemplateRef<any>) {
  }

}
