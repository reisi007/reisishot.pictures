var
    gulp = require('gulp'),
    browserSync = require('browser-sync'),
    sourcemaps = require('gulp-sourcemaps'),
    $ = require('gulp-load-plugins')({lazy: true}),
    serveStatic = require('serve-static');

function autoprefixCss() {
    return $.autoprefixer('last 2 version', 'safari 5', 'ie 8', 'ie 9', 'opera 12.1', 'ios 6', 'android 4');
}

function cleanCss() {
    return $.cleanCss({compatibility: 'ie8'});
}

gulp.task('styles', function () {
    return gulp
        .src('./src/scss/main.scss')
        .pipe($.sass().on('error', $.sass.logError))
        .pipe(autoprefixCss())
        .pipe($.cleanCss())
        .pipe($.concat('styles.css'))
        .pipe(gulp.dest('generated/css'))
        .pipe(browserSync.reload({stream: true}));
});

function babelify() {
    return $.babel();
}

gulp.task('scripts', function () {
    return gulp
        .src([
            './src/js/bootstrap/*.js',
            './src/js/bootstrap/**/*.js',
            './src/js/modules/*.js',
            './src/js/modules/**/*.js'
        ])
        .pipe(babelify())
        .pipe($.plumber())
        .pipe($.concat('combined.min.js'))
        .pipe($.uglify())
        .pipe(gulp.dest('generated/js'))

});

gulp.task('copyStatic', function () {
    return gulp
        .src('./src/static/**/*', {dot: true})
        .pipe(gulp.dest('generated'))
});

gulp.task('browser-sync', ['styles', 'scripts'], function () {
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
    // Watch .html files
    gulp.watch("generated/**/*.html").on('change', browserSync.reload);
    // Watch .sass files
    gulp.watch(['src/scss/**/*.scss', 'src/scss/**/*.css'], ['styles', browserSync.reload]);
    // Watch .js files
    gulp.watch('src/js/**/*.js', ['scripts', browserSync.reload]);
    // Watch static files
    gulp.watch('src/static/**/*.*', ['copyStatic', browserSync.reload]);
});

gulp.task('default', function () {
    gulp.start(
        'copyStatic',
        'styles',
        'scripts',
        'browser-sync',
        'watch'
    );
});