const
    gulp = require('gulp'),
    browserSync = require('browser-sync'),
    $ = require('gulp-load-plugins')({lazy: true}),
    serveStatic = require('serve-static'),
    frontend = './../frontend-framework/out/**/*';

gulp.task('copyStatic', function () {
    return gulp
        .src('./src/static/**/*', {dot: true})
        .pipe(gulp.dest('generated'))
});

gulp.task('copyFramework', function () {
    return gulp
        .src(frontend, {dot: true})
        .pipe(gulp.dest('generated'))
});

gulp.task('browser-sync', function () {
    browserSync({
        server: {
            middleware: [
                serveStatic("./generated/")
            ],
            injectChanges: true
        }
    });
});

gulp.task('watch', function () {
    // Watch framework
    gulp.watch("generated/**/*.html").on('change', browserSync.reload);
    // Watch .html files
    gulp.watch(frontend, ['copyFramework', browserSync.reload]);
    // Watch static files
    gulp.watch('src/static/**/*.*', ['copyStatic', browserSync.reload]);
});

gulp.task('default', function () {
    gulp.start(
        'copyStatic',
        'copyFramework',
        'browser-sync',
        'watch'
    );
});