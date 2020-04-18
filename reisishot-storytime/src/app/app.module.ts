import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {EpaperOverviewComponent} from './epaper-overview/epaper-overview.component';
import {EPaperContainer} from './epaper-paper/epaper-container.component';
import {ReisishotSampleMagazineComponent} from './epapers/reisishot-sample-magazine/reisishot-sample-magazine.component';
import {SampleMagazineSampleArticleComponent} from './epapers/reisishot-sample-magazine/sample-magazine-sample-article/sample-magazine-sample-article.component';
import {EPaperPageDirective} from './epaper-paper/epaper-page/e-paper-page.directive';
import {MatToolbarModule} from "@angular/material/toolbar";
import {MatIconModule} from "@angular/material/icon";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {MatFormFieldModule} from "@angular/material/form-field";
import {FormsModule} from "@angular/forms";
import {MatSelectModule} from "@angular/material/select";

@NgModule({
  declarations: [
    AppComponent,
    EpaperOverviewComponent,
    EPaperContainer,
    ReisishotSampleMagazineComponent,
    SampleMagazineSampleArticleComponent,
    EPaperPageDirective
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    FormsModule,
    MatSelectModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {
}
