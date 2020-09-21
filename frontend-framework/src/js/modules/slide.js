define(function () {
    class BeforeAfter {
        constructor() {
            const all = document.querySelectorAll('.bal-container');
            for (let i = 0; i < all.length; i++) {
                const beforeAfterContainer = all[i];
                const before = beforeAfterContainer.querySelector('.bal-before');
                const beforeText = beforeAfterContainer.querySelector('.bal-beforePosition');
                const afterText = beforeAfterContainer.querySelector('.bal-afterPosition');
                const handle = beforeAfterContainer.querySelector('.bal-handle');
                let widthChange = 0;

                beforeAfterContainer
                    .querySelector('.bal-before-inset')
                    .setAttribute("style", "width: " + beforeAfterContainer.offsetWidth + "px;")
                window.onresize = function () {
                    beforeAfterContainer
                        .querySelector('.bal-before-inset')
                        .setAttribute("style", "width: " + beforeAfterContainer.offsetWidth + "px;")
                }
                before.setAttribute('style', "width: 50%;");
                handle.setAttribute('style', "left: 50%;");

                //touch screen event listener
                beforeAfterContainer.addEventListener("touchstart", (e) => {

                    beforeAfterContainer.addEventListener("touchmove", (e2) => {
                        const beforeAfterBoundingRect = beforeAfterContainer.getBoundingClientRect();
                        let containerWidth = beforeAfterContainer.offsetWidth;
                        let currentPoint = e2.changedTouches[0].clientX;

                        let startOfDiv = beforeAfterBoundingRect.x;

                        let modifiedCurrentPoint = currentPoint - startOfDiv;

                        if (modifiedCurrentPoint > 10 && modifiedCurrentPoint < beforeAfterContainer.offsetWidth - 10) {
                            let newWidth = modifiedCurrentPoint * 100 / containerWidth;

                            before.setAttribute('style', "width:" + newWidth + "%;");
                            afterText.setAttribute('style', "z-index: 1;");
                            handle.setAttribute('style', "left:" + newWidth + "%;");
                        }
                    });
                });

                //mouse move event listener
                beforeAfterContainer.addEventListener('mousemove', (e) => {
                    let containerWidth = beforeAfterContainer.offsetWidth;
                    widthChange = e.offsetX;
                    let newWidth = widthChange * 100 / containerWidth;

                    if (e.offsetX > 10 && e.offsetX < beforeAfterContainer.offsetWidth - 10) {
                        before.setAttribute('style', "width:" + newWidth + "%;");
                        afterText.setAttribute('style', "z-index:" + "1;");
                        handle.setAttribute('style', "left:" + newWidth + "%;");
                    }
                })
            }
        }
    }

    function resize() {
        const all = document.querySelectorAll('.bal-container');
        const margin = 2 * 12;
        const ww = 0.9 * window.innerWidth - margin, wh = 0.9 * window.innerHeight - margin;
        for (let i = 0; i < all.length; i++) {
            const beforeAfterContainer = all[i];
            const ratio = parseFloat(beforeAfterContainer.getAttribute("data-ratio"));
            let w, h;
            if (wh < ratio * ww) {
                // Too high, calculate based on hight
                h = wh;
                w = wh / ratio
            } else {
                w = ww;
                h = ww * ratio;
            }
            beforeAfterContainer.style.width = w + "px"
            beforeAfterContainer.style.height = h + "px"
        }
    }

    window.addEventListener("resize", resize);
    resize();
    window.addEventListener('load', function () {
        new BeforeAfter();
    });
});