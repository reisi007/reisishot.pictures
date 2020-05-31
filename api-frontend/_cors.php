<?php
(function () {
    $allowedOrigins = array(
        /** @lang RegExp */
        'https://([^.]+\.)?reisishot\.pictures',
        /** @lang RegExp */
        'https?://localhost(:\d{1,})?'
    );
    if (isset($_SERVER['HTTP_ORIGIN']) && $_SERVER['HTTP_ORIGIN'] != '') {
        foreach ($allowedOrigins as $allowedOrigin) {
            if (preg_match('#' . $allowedOrigin . '#', $_SERVER['HTTP_ORIGIN'])) {
                header('Access-Control-Allow-Origin: ' . $_SERVER['HTTP_ORIGIN']);
                header('Access-Control-Allow-Methods: GET, PUT, POST, DELETE, OPTIONS');
                header('Access-Control-Max-Age: 1000');
                header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
                break;
            }
        }
    }
    // Exit early so the page isn't fully loaded for options requests
    if (strtolower($_SERVER['REQUEST_METHOD']) == 'options') {
        exit();
    }
})();
