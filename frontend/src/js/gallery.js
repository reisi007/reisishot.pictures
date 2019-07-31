//'use strict';
(function () {
    const galleries = window.galleries = window.galleries || {};

    document.addEventListener('DOMContentLoaded', function () {
        const observer = window.lozad('.lazy', {
            loaded: function (el) {
                el.style = "";
                el.classList.add('loaded');
            }
        });
        observer.observe();
        initGallery();
    });

    function initGallery() {
        // Save all galleries to the window
        document.querySelectorAll("div#gallery").forEach(gallery => {
            gallery.querySelectorAll("picture").forEach(pictureElement => {
                const galleryName = gallery.getAttribute("data-name");
                const curGallery = galleries[galleryName] = galleries[galleryName] || [];
                curGallery.push({picture: pictureElement});
                const index = curGallery.length - 1;
                pictureElement.onclick = () => openGallery(galleryName, index);
            })
        });
        parseUrl();
    }

    function openGallery(galleryName, realIndex) {

        const curGallery = galleries[galleryName];
        const options = {
            galleryUID: galleryName,
            history: true,
            galleryPIDs: true
        };

        options.index = parseInt(realIndex);
        console.log("Opening gallery", galleryName, "at index", realIndex);

        const photoswipeContainer = document.querySelectorAll('.pswp')[0];
        const gallery = new PhotoSwipe(photoswipeContainer, PhotoSwipeUI_Default, curGallery, options);

        gallery.listen('beforeResize', function () {
            gallery.invalidateCurrItems();
        });

        // gettingData event fires each time PhotoSwipe retrieves image source & size
        gallery.listen('gettingData', function (index, item) {
            const realViewportWidth = gallery.viewportSize.x * window.devicePixelRatio,
                realViewportHeight = gallery.viewportSize.y * window.devicePixelRatio;

            const pictureTag = item.picture;
            const sourceElement = getFirstMatchingImageSource(pictureTag);

            item.src = sourceElement.srcset;
            item.w = sourceElement.getAttribute("data-w");
            item.h = sourceElement.getAttribute("data-h");
            item.pid = curGallery.length - index;
            item.name = pictureTag.getAttribute("data-url");
            console.log("sdf", item)
        });

        gallery.init();
    }

    /**
     * Returns the best matching source element
     * @param pictureElement
     */
    function getFirstMatchingImageSource(pictureElement) {
        const sourceElements = pictureElement.querySelectorAll("source");
        for (const sourceElement of sourceElements) {
            const mediaQuery = sourceElement.media;
            if (!mediaQuery) return sourceElement;
            const mediaQueryTester = window.matchMedia(mediaQuery);
            if (mediaQueryTester.matches) {
                return sourceElement;
            }

        }
        return sourceElements[sourceElements.length - 1];
    }

    function parseUrl() {
        const hash = window.location.hash.substring(1);
        const vars = hash.split('&');
        const params = {};

        vars.forEach(v => {
            if (!v)
                return;
            let kvp = v.split("=");
            params[kvp[0]] = kvp[1];
        });

        const gid = params["gid"];
        const gallery = galleries[gid];
        const pid = parseInt(params["pid"]);
        if (gallery)
            openGallery(gid, gallery.length - pid);
    }
})();