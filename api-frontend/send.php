<?php

handleCors();

$inputJSON = file_get_contents('php://input');
$inputJSON = json_decode($inputJSON, true);
if (sendMail($inputJSON)) {
    http_response_code(204);
} else {
    http_response_code(555);
}

function handleCors()
{
    header("Access-Control-Allow-Origin: *");
    header("Access-Control-Allow-Headers: *");
    header('Access-Control-Allow-Methods: POST OPTIONS');
    // Exit early so the page isn't fully loaded for options requests
    if (strtolower($_SERVER['REQUEST_METHOD']) == 'options') {
        exit();
    }
}

function sendMail($inputJSON)
{
    $from = $inputJSON["E-Mail"];
    $to = "florian@reisishot.pictures";
    // To send HTML mail, the Content-type header must be set
    $headers = 'MIME-Version: 1.0' . "\r\n";
    $headers .= 'Content-type: text/html; charset=iso-8859-1' . "\r\n";
// Create email headers
    $headers .= 'From: ' . $from . "\r\n" .
        'Reply-To: ' . $from . "\r\n" .
        'X-Mailer: PHP/' . phpversion();

    $message = '<html lang="de"><body>';
    $message .= '<h1>Neue Nachricht vom Kontaktformular!</h1>';
    foreach ($inputJSON as $key => $value) {
        $message .= "<b>" . $key . "</b><br/>"
            . $value . "<br/>\n";
    }
    $message .= '</body></html>';
    return mail($to, "[reisishot.pictures] Neue Anfrage:", $message, $headers);
}

?>