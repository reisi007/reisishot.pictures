<?php
include '_cors.php';

$inputJSON = file_get_contents('php://input');
$inputJSON = json_decode($inputJSON, true);
if (sendMail($inputJSON)) {
    http_response_code(204);
} else {
    http_response_code(555);
}

function sendMail($inputJSON)
{
    $css = file_get_contents("style_contract.css");
    $customer = utf8_decode($inputJSON["mail"]);
    $me = "florian@reisishot.pictures";
    $to = $me . ' ' . $customer;
    // To send HTML mail, the Content-type header must be set
    $headers = 'MIME-Version: 1.0' . "\r\n";
    $headers .= 'Content-type: text/html; charset=utf-8' . "\r\n";
    // Create email headers
    $headers .= 'From: ' . $me . "\r\n" .
        'Reply-To: ' . $me . "\r\n" .
        'X-Mailer: PHP/' . phpversion();

    $message = "<html lang='de'><head><style>$css</style></head><body>";
    $message .= $inputJSON["text"];
    $message .= '</body></html>';
    return mail($to, "[Reisishot Vertrag] Vertrag von " . $customer, $message, $headers);
}

?>