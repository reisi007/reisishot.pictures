import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {EPaperPageDirective} from './epaper-paper/epaper-page/e-paper-page.directive';
import {EPaperContainer} from './epaper-paper/epaper-container.component';
import {EpaperOverviewComponent} from './epaper-overview/epaper-overview.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {FormsModule} from '@angular/forms';
import {MatSelectModule} from '@angular/material/select';
import {RouterModule} from '@angular/router';


@NgModule({
  declarations: [
    EpaperOverviewComponent,
    EPaperContainer,
    EPaperPageDirective,
  ],
  exports: [
    EPaperContainer,
    EPaperPageDirective,
  ],
  imports: [
    CommonModule,
    BrowserAnimationsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    FormsModule,
    RouterModule,
  ],
})
export class EpaperModule {
}
