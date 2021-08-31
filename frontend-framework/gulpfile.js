const
    gulp = require('gulp'),
    purgecss = require('gulp-purgecss'),
    $ = require('gulp-load-plugins')({lazy: true}),
    sites = ['reisishot.pictures', 'goto.reisishot.pictures', 'portrait.reisishot.pictures', 'couples.reisishot.pictures', 'boudoir.reisishot.pictures'];


gulp.task('styles', function () {
    return gulp
        .src('./src/scss/main.scss')
        .pipe($.sass().on('error', $.sass.logError))
        .pipe($.cleanCss())
        .pipe(purgecss({
            content: ['./generated/js/*.js'].concat(sites.map(s => '../upload/' + s + '/**/*.html'))
        }))
        .pipe($.concat('styles.css'))
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
    gulp.watch(['src/scss/**/*.scss', 'src/scss/**/*.css'], gulp.parallel('styles'));
    //Watch HTML files for changed classes / ids / tags
    gulp.watch(
        sites.map(s => '../upload/' + s + '/**/*.html'),
        gulp.parallel('styles')
    )
    // Watch .js files
    gulp.watch([
            './src/js/bootstrap/*.js',
            './src/js/bootstrap/**/*.js',
            './src/js/modules/*.js',
            './src/js/modules/**/*.js'
        ],
        gulp.parallel('scriptsDev')
    );
    // Watch static files
    gulp.watch('src/static/**/*.*', gulp.parallel('copyStatic'));
    gulp.watch('src/static_css/**/*.*', gulp.parallel('copyStaticCss'));
});

gulp.task('default', gulp.parallel(
    'copyStatic',
    'copyStaticCss',
    'scriptsDev',
    'styles',
    'watch'
));

gulp.task('release', gulp.parallel('copyStatic', 'scripts', 'styles'));

gulp.task('copyRelease', gulp.parallel('copyReleaseInternal', 'copyStaticCss'));
