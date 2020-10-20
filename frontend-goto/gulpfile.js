const
    gulp = require('gulp'),
    browserSync = require('browser-sync'),
    $ = require('gulp-load-plugins')({lazy: true}),
    serveStatic = require('serve-static'),
    frameworkStatic = './../frontend-framework/out/**/*',
    frameworkJsCss = './../frontend-framework/generated/**/*';

gulp.task('copyStatic', function () {
    return gulp
        .src('./src/static/**/*', {dot: true})
        .pipe(gulp.dest('generated'))
});

gulp.task('copyFrameworkStatic', function () {
    return gulp
        .src(frameworkStatic, {dot: true})
        .pipe(gulp.dest('generated'))
});

gulp.task('copyFrameworkJsCss', function () {
    return gulp
        .src(frameworkJsCss, {dot: true})
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
    // Watch static files
    gulp.watch(frameworkStatic, ['copyFrameworkStatic', browserSync.reload]);
    // Watch .js / .css  files
    gulp.watch(frameworkJsCss, ['copyFrameworkJsCss', browserSync.reload]);
    // Watch static files
    gulp.watch('src/static/**/*.*', ['copyStatic', browserSync.reload]);
});

gulp.task('default', function () {
    gulp.start(
        'copyStatic',
        'copyFrameworkStatic',
        'copyFrameworkJsCss',
        'browser-sync',
        'watch'
    );
});

gulp.task('release', function () {
    gulp.start(
        'copyStatic',
        'copyFrameworkStatic'
    );
});