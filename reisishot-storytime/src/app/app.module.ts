import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {ReisishotSampleMagazineComponent} from './app-epapers/reisishot-sample-magazine/reisishot-sample-magazine.component';
import {SampleMagazineSampleArticleComponent} from './app-epapers/reisishot-sample-magazine/sample-magazine-sample-article/sample-magazine-sample-article.component';
import {EpaperModule} from '../epaper/epaper.module';

@NgModule({
  declarations: [
    AppComponent,
    ReisishotSampleMagazineComponent,
    SampleMagazineSampleArticleComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    EpaperModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {
}
