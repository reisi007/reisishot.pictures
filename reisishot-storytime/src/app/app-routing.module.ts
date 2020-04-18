import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {EpaperOverviewComponent} from "./epaper-overview/epaper-overview.component";
import {ReisishotSampleMagazineComponent} from "./epapers/reisishot-sample-magazine/reisishot-sample-magazine.component";


const routes: Routes = [
  {path: '', component: EpaperOverviewComponent},
  {path: 'sample', redirectTo: '/sample/1', pathMatch: 'full'},
  {path: 'sample/:page', component: ReisishotSampleMagazineComponent},
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
