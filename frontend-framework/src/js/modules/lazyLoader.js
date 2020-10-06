define('lazyLoader', ['lozad'], function (lozad) {
    const observer = lozad('.lazy', {
        rootMargin: "540px 0px 0px 1620px",
        loaded: function (el) {
            el.parentElement.parentElement.classList.contains('single');
            el.style = "";
            el.classList.add('loaded');
        }
    });
    document.addEventListener('DOMContentLoaded', function () {
        observer.observe();
    });
    return observer;
});