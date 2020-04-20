import {Directive, TemplateRef} from '@angular/core';

@Directive({
  selector: '[ePaper]',
})
export class EPaperDirective {

  constructor(public template: TemplateRef<any>) {
  }

}
