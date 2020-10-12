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
        const img = document.createElement("img")
        new ResizeObserver(function () {
            img.src = getImageUrl(loadImageInternally(elem, elem.offsetWidth))
        }).observe(elem)
        img.src = getImageUrl(loadImageInternally(elem, elem.offsetWidth))
        img.alt = elem.getAttribute("data-alt")
        elem.append(img)
    }


    document.addEventListener('DOMContentLoaded', function () {
        observer.observe();
    });
    return observer;
});