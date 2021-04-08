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
    connect = require('gulp-connect'),
    $ = require('gulp-load-plugins')({lazy: true}),
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
        .on('end', done)
});

gulp.task('serve', function () {
    connect.server({
        root: [outBase],
        port: 3000,
        livereload: true
    })
});

gulp.task('watch', function () {
    // Watch framework
    gulp.watch(outBase + '/**/*.html')
        .on('change', connect.reload);
    // Watch static files
    gulp.watch(mainStatic, gulp.series('copyFrameworkStatic'))
        .on('change', connect.reload);
    // Watch .js / .css  files
    gulp.watch(frameworkJsCss, gulp.series('copyFrameworkJsCss'))
        .on('change', connect.reload);
    // Watch static files
    gulp.watch(inBase + 'src/static/**/*.*', gulp.series('copyStatic'))
        .on('change', connect.reload);
});

gulp.task('default',
    gulp.parallel(
        'copyStatic',
        'copyFrameworkStatic',
        'copyFrameworkJsCss',
        'serve',
        'watch'
    ));

gulp.task('release',
    gulp.parallel(
        'copyStatic',
        'copyFrameworkStatic'
    ));