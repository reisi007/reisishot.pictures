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
    browserSync = require('browser-sync').create(),
    $ = require('gulp-load-plugins')({lazy: true}),
    target = arg.target,
    inBase = './../frontend-static/' + target + '/',
    outBase = './../upload/' + target + '',
    staticSource = './../frontend-framework/out/**/*',
    frameworkJsCss = './../frontend-framework/generated/**/*.*';

gulp.task('copyStatic', function (done) {
    gulp
        .src(inBase + 'src/static/**/*', {dot: true})
        .pipe(gulp.dest(outBase))
        .on('end', done)
});

gulp.task('copyFrameworkStatic', function (done) {
    gulp
        .src(staticSource, {dot: true})
        .pipe(gulp.dest(outBase))
        .on('end', done)
});

gulp.task('copyFrameworkJsCss', function (done) {
    gulp
        .src(frameworkJsCss, {dot: true})
        .pipe(gulp.dest(outBase))
        .on('end', done)
});

gulp.task('serve', function () {
    browserSync.init({
        files: outBase + '**/*.*',
        watchOptions: {
            reloadOnRestart: true,
            reloadThrottle: 2000,
            ignoreInitial: true

        },
        server: {
            watch: true,
            baseDir: outBase
        }
    });
});

gulp.task('watch', function () {
    // Watch framework
    gulp.watch(outBase + '/**/*.html')
        .on('change', browserSync.reload);
    // Watch static files
    gulp.watch(staticSource, gulp.series('copyFrameworkStatic'));
    // Watch .js / .css  files
    gulp.watch(frameworkJsCss, gulp.series('copyFrameworkJsCss'))
        .on('change', browserSync.reload);
    // Watch static files
    gulp.watch(inBase + 'src/static/**/*.*', gulp.series('copyStatic'));
});

gulp.task('default',
    gulp.series(
        gulp.parallel(
            'copyStatic',
            'copyFrameworkStatic',
            'copyFrameworkJsCss',
        ),
        gulp.parallel(
            'serve',
            'watch'
        )
    ));

gulp.task('release',
    gulp.parallel(
        'copyStatic',
        'copyFrameworkStatic'
    ));
