define(['jquery', 'canUseWebP', 'loadImage', 'photoswipe', 'PhotoSwipeUI_Reisishot'], function ($, canUseWebP, loadImage, Photoswipe, ui) {
    'use strict';
    const galleries = {};
    const galleyHtml = '<div class="pswp" tabindex="-1" role="dialog" aria-hidden="true"><div class="pswp__bg"></div><div class="pswp__scroll-wrap"><div class="pswp__container"><div class="pswp__item"></div><div class="pswp__item"></div><div class="pswp__item"></div></div><div class="pswp__ui pswp__ui--hidden"><div class="pswp__top-bar"><div class="pswp__counter"></div><button class="pswp__button pswp__button--close" shorttitle="Schließen (Esc)"></button><button class="pswp__button pswp__button--fs" shorttitle="Fullscreen anzeigen"></button><button class="pswp__button pswp__button--zoom" shorttitle="Zoomen"></button><button class="pswp__button pswp__button--details" shorttitle="Details"></button><div class="pswp__preloader"><div class="pswp__preloader__icn"><div class="pswp__preloader__cut"><div class="pswp__preloader__donut"></div></div></div></div></div><div class="pswp__share-modal pswp__share-modal--hidden pswp__single-tap"><div class="pswp__share-tooltip"></div></div><button class="pswp__button pswp__button--arrow--left" shorttitle="Vorheriges Bild"></button><button class="pswp__button pswp__button--arrow--right" shorttitle="Nächstes Bild"></button><div class="pswp__caption"><div class="pswp__caption__center"></div></div></div></div></div>';

    document.addEventListener('DOMContentLoaded', function () {
        initGallery();
    });

    function initGallery() {
        // Save all galleries to the window
        document.querySelectorAll("div.gallery").forEach(gallery => {
            gallery.querySelectorAll("div.lazy").forEach(pictureElement => {
                const galleryName = gallery.getAttribute("data-name");
                const pictureName = pictureElement.getAttribute("data-id");
                const curGallery = galleries[galleryName] = galleries[galleryName] || {};
                curGallery[pictureName] = {picture: pictureElement};
                pictureElement.onclick = () => openGallery(galleryName, pictureName);
            })
        });
        if (!$.isEmptyObject(galleries))
            appendGalleryHtml();
        parseUrl();
    }

    function openGallery(galleryName, pictureName) {
        const curGallery = galleries[galleryName];
        const options = {
            galleryUID: galleryName,
            history: true,
            galleryPIDs: true
        };

        options.index = Object.keys(curGallery).indexOf(pictureName);
        const photoswipeContainer = document.querySelector('.pswp');

        const gallery = new Photoswipe(photoswipeContainer, ui, Object.values(curGallery), options);

        gallery.listen('beforeResize', function () {
            gallery.invalidateCurrItems();
        });

        // gettingData event fires each time PhotoSwipe retrieves image source & size
        gallery.listen('gettingData', function (index, item) {
            const realViewportWidth = gallery.viewportSize.x * window.devicePixelRatio,
                realViewportHeight = gallery.viewportSize.y * window.devicePixelRatio;
            console.log(item)
            const pic = item.picture;


            const data = loadImage(pic, realViewportWidth);
            item.src = canUseWebP() ? data.webp : data.jpg;
            item.w = data.w;
            item.h = data.h;
            item.pid = item.name = pic.getAttribute("data-id");
            item.url = pic.getAttribute("data-url");

        });

        gallery.init();
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
        const pid = params["pid"];
        if (gallery)
            openGallery(gid, pid);
    }

    function appendGalleryHtml() {
        document.body.appendChild($.parseHTML(galleyHtml)[0]);
    }
});