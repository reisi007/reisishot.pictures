define('loadImage', ['lozad'], function (lozad) {

    const sizes = [
        "data-small",
        "data-embed",
        "data-thumb",
        "data-medium",
        "data-large",
        "data-xlarge"
    ]

    function loadImageInternally(elem, width) {
        const cur = parseAttribute(elem, "cur") || {w: -1}
        if (cur.w >= width)
            return;

        for (let size of sizes) {
            const data = parseAttribute(elem, size);
            let cW = data.w;
            if (cW >= width) {
                return data

            }
        }
        return parseAttribute(elem, sizes[sizes.length - 1]);
    }

    function parseAttribute(elem, attr) {
        if (elem[attr])
            return elem[attr];
        return elem[attr] = JSON.parse(elem.getAttribute(attr));
    }

    return loadImageInternally;
})