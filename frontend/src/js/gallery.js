//'use strict';
$ = $ || window.jQuery;
(function () {
    const galleries = window.galleries = window.galleries || {};
    const galleyHtml = '<div class="pswp" tabindex="-1" role="dialog" aria-hidden="true"><div class="pswp__bg"></div><div class="pswp__scroll-wrap"><div class="pswp__container"><div class="pswp__item"></div><div class="pswp__item"></div><div class="pswp__item"></div></div><div class="pswp__ui pswp__ui--hidden"><div class="pswp__top-bar"><div class="pswp__counter"></div><button class="pswp__button pswp__button--close" shorttitle="Schließen (Esc)"></button><button class="pswp__button pswp__button--fs" shorttitle="Fullscreen anzeigen"></button><button class="pswp__button pswp__button--zoom" shorttitle="Zoomen"></button><button class="pswp__button pswp__button--details" shorttitle="Details"></button><div class="pswp__preloader"><div class="pswp__preloader__icn"><div class="pswp__preloader__cut"><div class="pswp__preloader__donut"></div></div></div></div></div><div class="pswp__share-modal pswp__share-modal--hidden pswp__single-tap"><div class="pswp__share-tooltip"></div></div><button class="pswp__button pswp__button--arrow--left" shorttitle="Vorheriges Bild"></button><button class="pswp__button pswp__button--arrow--right" shorttitle="Nächstes Bild"></button><div class="pswp__caption"><div class="pswp__caption__center"></div></div></div></div></div>';
    const googleAnalytics = '<script>window.dataLayer=window.dataLayer||[];function gtag(){dataLayer.push(arguments);}gtag("js",new Date());' +
        'gtag("config","UA-120917271-1",{"anonymize_ip":true});</script>';

    function initGoogleAnalytics() {
        window.dataLayer = window.dataLayer || [];

        function gtag() {
            dataLayer.push(arguments);
        }

        gtag('js', new Date());
        gtag('config', 'UA-120917271-1');
    }

    document.addEventListener('DOMContentLoaded', function () {
        initGoogleAnalytics();

        const observer = window.lozad('.lazy', {
            rootMargin: "1080px 0px 0px 0px",
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
        if (!$.isEmptyObject(galleries))
            appendGalleryHtml();
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

    function appendGalleryHtml() {
        document.body.append($.parseHTML(galleyHtml)[0]);
    }
})();