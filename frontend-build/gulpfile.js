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
    inBase = './../' + arg.target + '/',
    outBase = './../upload/' + arg.target + '',
    frameworkStatic = './../frontend-framework/out/**/*',
    frameworkJsCss = './../frontend-framework/generated/**/*';

gulp.task('copyStatic', function () {
    return gulp
        .src(inBase + 'src/static/**/*', {dot: true})
        .pipe(gulp.dest(outBase))
});

gulp.task('copyFrameworkStatic', function () {
    return gulp
        .src(frameworkStatic, {dot: true})
        .pipe(gulp.dest(outBase))
});

gulp.task('copyFrameworkJsCss', function () {
    return gulp
        .src(frameworkJsCss, {dot: true})
        .pipe(gulp.dest(outBase))
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
    gulp.watch(frameworkStatic, gulp.parallel('copyFrameworkStatic', browserSync.reload));
    // Watch .js / .css  files
    gulp.watch(frameworkJsCss, gulp.parallel('copyFrameworkJsCss', browserSync.reload));
    // Watch static files
    gulp.watch(inBase + 'src/static/**/*.*', gulp.parallel('copyStatic', browserSync.reload));
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