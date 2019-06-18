//'use strict';
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const observer = window.lozad('.lazy', {
            loaded: function (el) {
                console.log("Lazy loaded element ", el)
            }
        });
        observer.observe();
    });
})();