const
    gulp = require('gulp'),
    $ = require('gulp-load-plugins')({lazy: true});

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
        .pipe(gulp.dest('generated/css'));
});

gulp.task('styles-boudoir', function () {
    return gulp
        .src('./src/scss/main-boudoir.scss')
        .pipe($.sass().on('error', $.sass.logError))
        .pipe(autoprefixCss())
        .pipe($.cleanCss())
        .pipe($.concat('styles-boudoir.css'))
        .pipe(gulp.dest('generated/css'));
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

gulp.task('scriptsDev', function () {
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
        .pipe(gulp.dest('generated/js'))

});

gulp.task('copyStatic', function () {
    return gulp
        .src('./src/static/**/*', {dot: true})
        .pipe(gulp.dest('out'))
});

gulp.task('copyStaticCss', function () {
    return gulp
        .src('./src/static_css/**/*', {dot: true})
        .pipe(gulp.dest('./../upload/static.reisishot.pictures/static'))
});

gulp.task('copyReleaseInternal', function () {
    return gulp
        .src('./generated/**/*', {dot: true})
        .pipe(gulp.dest('./../upload/static.reisishot.pictures'))
});

gulp.task('watch', function () {
    // Watch .sass files
    gulp.watch(['src/scss/**/*.scss', 'src/scss/**/*.css'], gulp.parallel('styles', 'styles-boudoir'));
    // Watch .js files
    gulp.watch([
            './src/js/bootstrap/*.js',
            './src/js/bootstrap/**/*.js',
            './src/js/modules/*.js',
            './src/js/modules/**/*.js'
        ],
        gulp.parallel('scriptsDev')
    )
    ;
    // Watch static files
    gulp.watch('src/static/**/*.*', gulp.parallel('copyStatic'));
});

gulp.task('default', gulp.parallel(
    'copyStatic',
    'styles',
    'styles-boudoir',
    'scriptsDev',
    'watch'
));

gulp.task('release', gulp.parallel('copyStatic', 'styles', 'styles-boudoir', 'scripts'));

gulp.task('copyRelease', gulp.parallel('copyReleaseInternal', 'copyStaticCss'));