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
    $ = require('gulp-load-plugins')({lazy: true}),
    sass = require('gulp-sass')(require('sass')),
    target = arg.target,
    inBase = './../frontend-static/' + target + '/',
    outBase = './../upload/' + target;

gulp.task('styles', function () {
    return gulp
        .src('./src/scss/main.scss')
        .pipe(sass().on('error', sass.logError))
        .pipe($.cleanCss())
        .pipe($.purgecss({
            content: [`${outBase}/**/*.html`, `${outBase}/js/*.js`]
        }))
        .pipe($.concat('styles.css'))
        .pipe(gulp.dest(`${outBase}/css`));
});

gulp.task('stylesDev', function () {
    return gulp
        .src('./src/scss/main.scss')
        .pipe(sass().on('error', sass.logError))
        .pipe($.concat('styles.css'))
        .pipe(gulp.dest(`${outBase}/css`));
});

gulp.task('scripts', function () {
    return gulp
        .src([
            './src/js/bootstrap/*.js',
            './src/js/bootstrap/**/*.js',
            './src/js/modules/*.js',
            './src/js/modules/**/*.js'
        ])
        .pipe($.babel({
            presets: ['@babel/env']
        }))
        .pipe($.concat('combined.min.js'))
        .pipe($.terser())
        .pipe(gulp.dest(`${outBase}/js`))

});

gulp.task('scriptsDev', function () {
    return gulp
        .src([
            './src/js/bootstrap/*.js',
            './src/js/bootstrap/**/*.js',
            './src/js/modules/*.js',
            './src/js/modules/**/*.js'
        ])
        .pipe($.concat('combined.min.js'))
        .pipe(gulp.dest(`${outBase}/js`))

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
        .pipe(gulp.dest(`${outBase}`));

    gulp.src('./src/static_css/**/*', {dot: true})
        .on('end', doneLogic)
        .pipe(gulp.dest(`${outBase}/css`));

    gulp.src(`${inBase}/src/static/**/*`)
        .on('end', doneLogic)
        .pipe(gulp.dest(`${outBase}`));
});

gulp.task('watch', function () {
    // Watch .sass files
    gulp.watch(['src/scss/**/*.scss'], gulp.series('stylesDev'))

    gulp.watch(['src/scss/**/*.html'], gulp.series('stylesDev'))

    // Watch .js files
    gulp.watch(
        [
            './src/js/bootstrap/*.js',
            './src/js/bootstrap/**/*.js',
            './src/js/modules/*.js',
            './src/js/modules/**/*.js'
        ],
        gulp.series('scriptsDev')
    );
});

gulp.task('default', gulp.series(
    gulp.parallel(
        'copyStatic',
        'scriptsDev',
    ),
    gulp.parallel(
        'stylesDev',
        'watch'
    )
));

gulp.task('release',
    gulp.series(
        gulp.parallel('copyStatic', 'scripts'),
        'styles'
    ));
