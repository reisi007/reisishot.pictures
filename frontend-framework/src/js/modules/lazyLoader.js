define('lazyLoader', ['lozad', 'canUseWebP'], function (lozad, canUseWebP) {

    const sizes = [
        "data-small",
        "data-embed",
        "data-thumb",
        "data-medium",
        "data-large",
        "data-xlarge"
    ]

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

    function loadImage(elem) {
        new ResizeObserver(function () {
            console.log("Resize")
            loadImageInternally(elem)
        }).observe(elem)
        loadImageInternally(elem)
    }

    function loadImageInternally(elem) {
        const webp = canUseWebP();

        const width = elem.parentElement.offsetWidth;


        const cur = parseAttribute(elem, "cur") || {w: -1}
        if (cur.w >= width)
            return;

        function loadImage(data) {
            elem.src = webp ? data.webp : data.jpg;
            elem.cur = data;
        }

        for (let size of sizes) {
            const data = parseAttribute(elem, size);
            let cW = data.w;
            if (cW >= width) {
                console.log("Loading", width, data)
                loadImage(data)
                return;
            }
        }
        loadImage(parseAttribute(elem, sizes[sizes.length - 1]))
    }

    function parseAttribute(elem, attr) {
        if (elem[attr])
            return elem[attr];
        return elem[attr] = JSON.parse(elem.getAttribute(attr));
    }

    document.addEventListener('DOMContentLoaded', function () {
        observer.observe();
    });
    return observer;
});