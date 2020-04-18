import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {EpaperOverviewComponent} from './epaper-overview/epaper-overview.component';
import {EPaperContainer} from './epaper-paper/epaper-container.component';
import {ReisishotSampleMagazineComponent} from './epapers/reisishot-sample-magazine/reisishot-sample-magazine.component';
import {EpaperPageComponent} from './epaper-paper/epaper-page/epaper-page.component';
import {SampleMagazineSampleArticleComponent} from './epapers/reisishot-sample-magazine/sample-magazine-sample-article/sample-magazine-sample-article.component';

@NgModule({
  declarations: [
    AppComponent,
    EpaperOverviewComponent,
    EPaperContainer,
    ReisishotSampleMagazineComponent,
    EpaperPageComponent,
    SampleMagazineSampleArticleComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {
}
