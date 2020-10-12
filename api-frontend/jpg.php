<?php

function imageResize($url, $width, $height, $q)
{
    header('Content-type: image/jpeg');

    list($width_orig, $height_orig) = getimagesize($url);

    // Do NOT upscale image
    if ($width > $width_orig)
        $width = $width_orig;
    if ($height > $height_orig)
        $height = $height_orig;

    // Ensure quaity is in bounds
    if ($q < 0 || $q > 100)
        $q = 80;

    $ratio_orig = $width_orig / $height_orig;

    if ($width / $height > $ratio_orig) {
        $width = $height * $ratio_orig;
    } else {
        $height = $width / $ratio_orig;
    }

    // This resamples the image
    $image_p = imagecreatetruecolor($width, $height);
    $image = imagecreatefromjpeg($url);
    imagecopyresampled($image_p, $image, 0, 0, 0, 0, $width, $height, $width_orig, $height_orig);

    // Output the image
    imagejpeg($image_p, null, $q);

}

//works with both POST and GET
$method = $_SERVER['REQUEST_METHOD'];

if ($method == 'GET') {
    imageResize('../images.data/' . $_GET['url'], $_GET['w'] ?? PHP_INT_MAX, $_GET['h'] ?? PHP_INT_MAX, $_GET['q'] ?? -1);
} elseif ($method == 'POST') {
    imageResize('../images.data/' . $_POST['url'], $_POST['w'] ?? PHP_INT_MAX, $_POST['h'] ?? PHP_INT_MAX, $_POST['q'] ?? -1);
}
?>