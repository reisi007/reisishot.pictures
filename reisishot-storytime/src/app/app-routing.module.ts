import {NgModule, Type} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {ReisishotSampleMagazineComponent} from './app-epapers/reisishot-sample-magazine/reisishot-sample-magazine.component';
import {EpaperOverviewComponent} from '../epaper/epaper-overview/epaper-overview.component';


const routes: Routes = [
  {path: '', component: EpaperOverviewComponent},
  ...calculateMagazineRoutes(
    {'sample': ReisishotSampleMagazineComponent},
  ),
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {
}

function calculateMagazineRoutes(kiosks: { [key: string]: Type<any> }, prefix: string = ''): Routes {
  prefix = prefix.trim();
  if(prefix.length === 0) {
    prefix = '';
  }
  else if(!prefix.endsWith('/')) {
    prefix = prefix + '/';
  }

  const routes: Routes = [];
  for(let objectKey of Object.keys(kiosks)) {
    const component = kiosks[objectKey];
    routes.push(
      {path: prefix + objectKey + '/:page', component: component},
      {path: prefix + objectKey, pathMatch: 'full', redirectTo: prefix + objectKey + '/1'},
    );
  }
  return routes;
}