define('wp_chat', [], function () {

    window.fbAsyncInit = function () {
        FB.init({
            xfbml: true,
            version: 'v9.0'
        });
    };

    document.addEventListener('DOMContentLoaded', function () {
        (function (d, s, id) {
            var js, fjs = d.getElementsByTagName(s)[0];
            if (d.getElementById(id)) return;
            js = d.createElement(s);
            js.id = id;
            js.src = 'https://connect.facebook.net/de_DE/sdk/xfbml.customerchat.js';
            fjs.parentNode.insertBefore(js, fjs);
        }(document, 'script', 'facebook-jssdk'));
    });

});

