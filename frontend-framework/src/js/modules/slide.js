define(function () {
    function initComparisons() {
        var overlays;
        /* Find all overlays with an "overlay" class: */
        overlays = document.getElementsByClassName("img-comp-overlay");
        for (let i = 0; i < overlays.length; i++) {
            /* Once for each "overlay" element:
            pass the "overlay" element as a parameter when executing the compareImages function: */
            let e = overlays[i];
            let initialScale = e.getAttribute("data-initial");
            if (!initialScale)
                initialScale = 0.5;
            compareImages(e, initialScale);
        }

        function compareImages(img, initialScale) {
            var slider, img, clicked = 0, w, h;
            /* Get the width and height of the img element */
            w = img.offsetWidth;
            h = img.offsetHeight;
            /* Set the width of the img element to 50%: */
            // img.style.width = (w / 2) + "px";
            img.style.width = w * initialScale + "px";
            img.parentElement.style.width = w + "px";
            /* Create slider: */
            slider = document.createElement("DIV");
            slider.setAttribute("class", "img-comp-slider");
            /* Insert slider */
            img.parentElement.insertBefore(slider, img);
            /* Position the slider in the middle: */
            slider.style.top = (h / 2) - (slider.offsetHeight / 2) + "px";
            // slider.style.left = (w / 2) - (slider.offsetWidth / 2) + "px";
            slider.style.left = (w * initialScale) - (slider.offsetWidth / 2) + "px";
            /* Execute a function when the mouse button is pressed: */
            slider.addEventListener("mousedown", slideReady);
            /* And another function when the mouse button is released: */
            window.addEventListener("mouseup", slideFinish);
            /* Or touched (for touch screens: */
            slider.addEventListener("touchstart", slideReady);
            /* And released (for touch screens: */
            window.addEventListener("touchend", slideFinish);

            function slideReady(e) {
                /* Prevent any other actions that may occur when moving over the image: */
                e.preventDefault();
                /* The slider is now clicked and ready to move: */
                clicked = 1;
                /* Execute a function when the slider is moved: */
                window.addEventListener("mousemove", slideMove);
                window.addEventListener("touchmove", slideMove);
            }

            function slideFinish() {
                /* The slider is no longer clicked: */
                clicked = 0;
            }

            function slideMove(e) {
                var pos;
                /* If the slider is no longer clicked, exit this function: */
                if (clicked === 0) return false;
                /* Get the cursor's x position: */
                pos = getCursorPos(e)
                /* Prevent the slider from being positioned outside the image: */
                if (pos < 0) pos = 0;
                if (pos > w) pos = w;
                /* Execute a function that will resize the overlay image according to the cursor: */
                slide(pos);
            }

            function getCursorPos(e) {
                var a, x = 0;
                e = e || window.event;
                /* Get the x positions of the image: */
                a = img.getBoundingClientRect();
                /* Calculate the cursor's x coordinate, relative to the image: */
                x = e.pageX - a.left;
                /* Consider any page scrolling: */
                x = x - window.pageXOffset;
                return x;
            }

            function slide(x) {
                /* Resize the image: */
                img.style.width = x + "px";
                /* Position the slider: */
                slider.style.left = img.offsetWidth - (slider.offsetWidth / 2) + "px";
            }
        }
    }

    window.addEventListener('load', function () {
        initComparisons();
    });
});