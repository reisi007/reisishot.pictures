const
    gulp = require('gulp'),
    purgecss = require('gulp-purgecss'),
    $ = require('gulp-load-plugins')({lazy: true}),
    sites = ['reisishot.pictures', 'goto.reisishot.pictures', 'portrait.reisishot.pictures'];

function autoprefixCss() {
    return $.autoprefixer('last 2 version', 'safari 5', 'ie 8', 'ie 9', 'opera 12.1', 'ios 6', 'android 4');
}

gulp.task('styles', function () {

    return gulp
        .src('./src/scss/main.scss')
        .pipe($.sass().on('error', $.sass.logError))
        .pipe(autoprefixCss())
        .pipe($.cleanCss())
        .pipe(purgecss({
            content: [
                '../upload/boudoir.reisishot.pictures/**/*.html',
                './generated/js/*.js'
            ]
        }))
        .pipe($.concat('styles.css'))
        .pipe(gulp.dest('generated/css'));
});

gulp.task('styles-boudoir', function () {
    return gulp
        .src('./src/scss/main-boudoir.scss')
        .pipe($.sass().on('error', $.sass.logError))
        .pipe(autoprefixCss())
        .pipe($.cleanCss())
        .pipe(purgecss({
            content: ['./generated/js/*.js'].concat(sites.map(s => '../upload/' + s + '/**/*.html'))
        }))
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

gulp.task('copyStatic', function (done) {
    let doneCount = 0
    const taskCount = 3;

    function doneLogic() {
        doneCount++;
        if (doneCount === taskCount)
            done();
    }

    gulp.src('./src/static/**/*', {dot: true})
        .on('end', doneLogic)
        .pipe(gulp.dest('out'));

    gulp.src('./src/static_boudoir/**/*', {dot: true})
        .on('end', doneLogic)
        .pipe(gulp.dest('out_boudoir'));

    gulp.src('./src/static_css/**/*', {dot: true})
        .on('end', doneLogic)
        .pipe(gulp.dest('out/css'));
});

gulp.task('copyStaticCss', function (done) {
    return gulp
        .src('./src/static_css/**/*', {dot: true})
        .on('end', function () {
            done();
        })
        .pipe(gulp.dest('./../upload/static.reisishot.pictures/static'))
});

gulp.task('copyReleaseInternal', function (done) {
    return gulp
        .src('./generated/**/*', {dot: true})
        .on('end', function () {
            done();
        })
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
    gulp.watch('src/static_css/**/*.*', gulp.parallel('copyStaticCss'));
});

gulp.task('default', gulp.parallel(
    'copyStatic',
    'copyStaticCss',
    'scriptsDev',
    'styles',
    'styles-boudoir',
    'watch'
));

gulp.task('release', gulp.parallel('copyStatic', 'scripts', 'styles', 'styles-boudoir'));

gulp.task('copyRelease', gulp.parallel('copyReleaseInternal', 'copyStaticCss'));
