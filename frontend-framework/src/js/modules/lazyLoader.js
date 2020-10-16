define('lazyLoader', ['lozad', 'canUseWebP', 'loadImage'], function (lozad, webp, loadImageInternally) {

    const observer = lozad('.lazy', {
        rootMargin: "540px 0px 0px 1620px",
        load: function (elem) {
            if (elem.getAttribute("data-sizes"))
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
        const ignoreHeight = elem.classList.contains("only-w");
        new ResizeObserver(function () {
            img.src = getImageUrl(loadImageInternally(elem, elem.offsetWidth, ignoreHeight ? null : elem.offsetHeight))
        }).observe(elem)
        let height = elem.offsetHeight;
        if (height < 50 || ignoreHeight)
            height = null
        img.src = getImageUrl(loadImageInternally(elem, elem.offsetWidth, height))
        img.alt = elem.getAttribute("data-alt")
        elem.append(img)
    }


    document.addEventListener('DOMContentLoaded', function () {
        observer.observe();
    });
    return function () {
        observer.observe();
    }
});