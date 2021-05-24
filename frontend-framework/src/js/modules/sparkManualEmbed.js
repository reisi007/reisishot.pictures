define(['trackAction'], function (trackAction) {
    const a = document.querySelectorAll(".asp-embed-link")

    function getNameFromATag(cur, fallback) {
        const img = cur.getElementsByTagName("img")[0];
        if (img)
            return img.getAttribute("alt");
        const dataAlt = cur.getAttribute("data-alt")
        if (dataAlt)
            return dataAlt;
        return fallback;
    }

    for (let idx = 0, e = a.length; idx < e; idx++) {
        let cur = a[idx];
        const url = cur.getAttribute("href");
        cur.removeAttribute("href")
        const alt = getNameFromATag(cur, url);
        cur.onclick = () => window.AdobeSparkPage.showFeature(url, alt)
    }

    const d = "SparkPage-feature-overlay";
    const e = "SparkPage-feature-iframe";

    const sparkViewer = window.AdobeSparkPage = {
        showFeature: function (url, alt) {
            let overlay = document.getElementById(d);
            let iframe = document.getElementById(e);
            if (!overlay || !iframe) {
                overlay = document.createElement("div");
                overlay.id = d;
                const j = 1e6;
                overlay.style.cssText = "z-index:" + j + ";position:fixed;top:0;right:0;bottom:0;left:0;background-color:rgba(26,34,38,0.9);text-align:center;";
                const divElement = document.createElement("div");
                divElement.innerHTML = '<div class="close-link" style="cursor:pointer;position: absolute; top: 10px; right: 15px; width:16px;height:16px;"><style>.close-link-icon{fill:#9aa6af} .close-link:hover .close-link-icon{fill:#dce0e3}</style><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><path class="close-link-icon" d="M5.75,0L0,5.75,2.94,8.68l7.26,7.38L2.94,23.32,0,26.13,5.75,32l2.94-2.94,7.38-7.38,7.26,7.38L26.13,32,32,26.13l-2.94-2.81-7.38-7.26,7.38-7.38L32,5.75,26.13,0,23.32,2.94l-7.26,7.26L8.68,2.94Z"/></svg></div>';
                const close = divElement.childNodes[0];
                overlay.appendChild(close);
                const n = document.createElement("div");
                n.style.cssText = "position: absolute; top:35px; left: 15px;bottom: 35px; right: 15px;";
                overlay.appendChild(n);
                iframe = document.createElement("iframe");
                iframe.id = e;
                iframe.style.cssText = "display:inline-block;width:100%;height:100%;border:none;background-color:black;";
                n.appendChild(iframe);
                document.body.appendChild(overlay);
                overlay.addEventListener("click", a => a.target === overlay && sparkViewer.hideFeatureFrame());
                close.addEventListener("click", sparkViewer.hideFeatureFrame);
            }

            trackAction({group: "spark", action: "open", id: alt})
            if (url) {
                overlay.style.display = "block";
                iframe.src = url + "?ref=" + encodeURIComponent(window.location.href) + "&embed_type=overlay&context=lightbox";
                iframe.focus();
                window.AdobeSparkPage.lastBodyOverflowValue = undefined;
                if (window.getComputedStyle) {
                    window.AdobeSparkPage.lastBodyOverflowValue = window.getComputedStyle(document.body, null).getPropertyValue("overflow");
                    document.body.style.overflow = "hidden";
                }
            }

        }, hideFeatureFrame: function () {
            const overlay = document.getElementById(d);
            const iFrame = document.getElementById(e);
            if (overlay) {
                overlay.style.display = "none";
                if (window.AdobeSparkPage.lastBodyOverflowValue) {
                    document.body.style.overflow = window.AdobeSparkPage.lastBodyOverflowValue;
                    window.AdobeSparkPage.lastBodyOverflowValue = undefined;
                }
            }
            if (iFrame) {
                iFrame.src = "about:blank"
            }
        }
    };
});
