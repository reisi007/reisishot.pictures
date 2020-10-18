define('loadImage', [], function () {

    function loadImageInternally(elem, width, height) {
        function fits(data) {
            return data.w >= width && (height === null || data.h >= height)
        }

        const cur = parseAttribute(elem, "cur") || {w: -1, h: -1}
        if (fits(cur))
            return cur;


        if (cur.w >= width || (height != null && cur.h >= height))
            return cur;
        const sizes = elem.getAttribute("data-sizes")
        for (let i = 0; i < sizes; i++) {
            const data = parseAttribute(elem, "data-" + i);
            if (fits(data)) {
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