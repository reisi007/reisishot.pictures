define('lazyLoader', ['lozad', 'canUseWebP', 'loadImage'], function (lozad, webp, loadImageInternally) {

    const observer = lozad('.lazy', {
        rootMargin: "540px 0px 0px 1620px",
        load: function (elem) {
            if (elem.getAttribute("data-small"))
                loadImage(elem)
            else
                lozad.defaultConfig.load(elem)
        },
        loaded: function (el) {
            el.parentElement.parentElement.classList.contains('single');
            el.style = "";
            el.classList.add('loaded');
        }
    });

    function getImageUrl(data) {
        return webp() ? data.webp : data.jpg;
    }

    function loadImage(elem) {
        new ResizeObserver(function () {
            elem.src = getImageUrl(loadImageInternally(elem, elem.parentElement.offsetWidth))
        }).observe(elem)
        elem.src = getImageUrl(loadImageInternally(elem, elem.parentElement.offsetWidth))
    }


    document.addEventListener('DOMContentLoaded', function () {
        observer.observe();
    });
    return observer;
});