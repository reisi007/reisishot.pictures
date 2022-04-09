define('canUseWebP', [], function () {
    let _webp = null;

    function canUseWebP() {
        if (_webp === null)
            _webp = internalCheckWebPSupport();
        return _webp
    }

    function internalCheckWebPSupport() {
        const elem = document.createElement('canvas');
        if (!!(elem.getContext && elem.getContext('2d'))) {
            // was able or not to get WebP representation
            return elem.toDataURL('image/webp')
                .indexOf('data:image/webp') === 0;
        }

        // very old browser like IE 8, canvas not supported
        return false;
    }

    return canUseWebP
});