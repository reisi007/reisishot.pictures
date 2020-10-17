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
            const w = img.width;
            const h = calcHeight(img.height, ignoreHeight);
            img.src = getImageUrl(loadImageInternally(elem, w, h))
        }).observe(img)

        img.src = getImageUrl(loadImageInternally(elem, elem.offsetWidth, calcHeight(elem.offsetHeight, ignoreHeight)))
        img.alt = elem.getAttribute("data-alt")
        elem.append(img)
    }

    function calcHeight(height, ignoreHeight) {
        if (height < 50 || ignoreHeight)
            height = null
        return height
    }

    document.addEventListener('DOMContentLoaded', function () {
        observer.observe();
    });
    return function () {
        observer.observe();
    }
});