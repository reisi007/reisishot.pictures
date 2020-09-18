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
    $from = utf8_decode($inputJSON["E-Mail"]);
    $betreff = utf8_decode($inputJSON["Betreff"]);
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
        $message .= "<b>" . utf8_decode($key) . "</b><br/>"
            . utf8_decode($value) . "<br/>\n";
    }
    $message .= '</body></html>';
    return mail($to, "[Kontaktformular] Neue Anfrage: " . $betreff, $message, $headers);
}

?>