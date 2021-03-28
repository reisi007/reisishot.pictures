// fetch command line arguments
const arg = (argList => {
    let arg = {}, a, opt, thisOpt, curOpt;
    for (a = 0; a < argList.length; a++) {
        thisOpt = argList[a].trim();
        opt = thisOpt.replace(/^-+/, '');

        if (opt === thisOpt) {
            // argument value
            if (curOpt) arg[curOpt] = opt;
            curOpt = null;
        } else {
            // argument name
            curOpt = opt;
            arg[curOpt] = true;
        }
    }
    return arg;
})(process.argv);

const
    gulp = require('gulp'),
    browserSync = require('browser-sync'),
    $ = require('gulp-load-plugins')({lazy: true}),
    serveStatic = require('serve-static'),
    target = arg.target,
    isBoudoir = target === 'boudoir.reisishot.pictures',
    inBase = './../' + target + '/',
    outBase = './../upload/' + target + '',
    mainStatic = './../frontend-framework/out/**/*',
    boudoirStatic = './../frontend-framework/out_boudoir/**/*',
    frameworkJsCss = './../frontend-framework/generated/**/*';

gulp.task('copyStatic', function (done) {
    gulp
        .src(inBase + 'src/static/**/*', {dot: true})
        .pipe(gulp.dest(outBase))
        .on('end', done)
});

gulp.task('copyFrameworkStatic', function (done) {
    const staticSource = isBoudoir ? boudoirStatic : mainStatic;

    gulp
        .src(staticSource, {dot: true})
        .pipe(gulp.dest(outBase))
        .on('end', done)
});

gulp.task('copyFrameworkJsCss', function (done) {
    gulp
        .src(frameworkJsCss, {dot: true})
        .pipe(gulp.dest(outBase))
        .on('end', function () {
            browserSync.reload();
            done()
        })
});

gulp.task('browser-sync', function () {
    browserSync({
        server: {
            middleware: [
                serveStatic(outBase)
            ],
            injectChanges: true
        }
    });
});

gulp.task('watch', function () {
    // Watch framework
    gulp.watch(outBase + '/**/*.html').on('change', browserSync.reload);
    // Watch static files
    gulp.watch(mainStatic, gulp.series('copyFrameworkStatic'));
    // Watch .js / .css  files
    gulp.watch(frameworkJsCss, gulp.series('copyFrameworkJsCss'));
    // Watch static files
    gulp.watch(inBase + 'src/static/**/*.*', gulp.series('copyStatic'))
});

gulp.task('default',
    gulp.parallel(
        'copyStatic',
        'copyFrameworkStatic',
        'copyFrameworkJsCss',
        'browser-sync',
        'watch'
    ));

gulp.task('release',
    gulp.parallel(
        'copyStatic',
        'copyFrameworkStatic'
    ));