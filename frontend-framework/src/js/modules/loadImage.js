define('loadImage', [], function () {

    function loadImageInternally(elem, width, height) {
        const cur = parseAttribute(elem, "cur") || {w: -1, h: -1}
        if (cur.w >= width || (height != null && cur.h >= height))
            return cur;
        const sizes = elem.getAttribute("data-sizes")
        for (let i = 0; i < sizes; i++) {
            const data = parseAttribute(elem, "data-" + i);
            let cW = data.w;
            let cH = data.h;
            if (cW >= width && (height == null || cH >= height)) {
                return data

            }
        }
        return parseAttribute(elem, "data-" + sizes - 1);
    }

    function parseAttribute(elem, attr) {
        if (elem[attr])
            return elem[attr];
        return elem[attr] = JSON.parse(elem.getAttribute(attr));
    }

    return loadImageInternally;
})