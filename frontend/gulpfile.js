var
    gulp = require('gulp'),
    browserSync = require('browser-sync'),
    $ = require('gulp-load-plugins')({lazy: true});

gulp.task('styles', function () {
    return gulp
        .src('./src/sass/main.scss')
        .pipe($.sass().on('error', $.sass.logError))
        .pipe($.autoprefixer('last 2 version', 'safari 5', 'ie 8', 'ie 9', 'opera 12.1', 'ios 6', 'android 4'))
        .pipe($.cleanCss())
        .pipe($.concat('app.css'))
        .pipe(gulp.dest('generated/css'))
        .pipe(browserSync.reload({stream: true}));
});

gulp.task('vendorStyles', function () {
    gulp.src('./src/css/**/*.css')
        .pipe($.concat('vendor.css'))
        .pipe(gulp.dest('generated/css'));
});

gulp.task('vendorScripts', function () {
    gulp.src('./src/js/vendor/**/*.js')
        .pipe(gulp.dest('generated/js/vendor'));
});

gulp.task('scripts', function () {
    return gulp
        .src([
            './src/js/!(vendor)**/!(app)*.js',
            './src/js/!(app)*.js'
        ])
        .pipe($.plumber())
        .pipe($.babel())
        .pipe($.concat('app.js'))
        //.pipe( $.uglify() )
        .pipe(gulp.dest('generated/js'))
        .pipe(browserSync.reload({stream: true}));
});

gulp.task('browser-sync', ['styles', 'scripts'], function () {
    browserSync({
        server: {
            baseDir: "./generated/",
            directory: false,
            injectChanges: true
        }
    });
});

gulp.task('deploy', function () {
    return gulp
        .src('./public/**/*')
        .pipe(ghPages());
});

gulp.task('watch', function () {
    // Watch .html files
    gulp.watch("generated/**/*.html").on('change', browserSync.reload);
    // Watch .sass files
    gulp.watch('src/sass/**/*.scss', ['styles', browserSync.reload]);
    // Watch .js files
    gulp.watch('src/js/*.js', ['scripts', browserSync.reload]);
    // Watch .js files
    gulp.watch('src/js/vendor/*', ['vendorScripts', browserSync.reload]);
});

gulp.task('default', function () {
    gulp.start(
        'vendorStyles',
        'styles',
        'vendorScripts',
        'scripts',
        'browser-sync',
        'watch'
    );
});