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
    $from = safeString($inputJSON["email"]);
    $betreff = safeString($inputJSON["subject"]);
    $to = "florian@reisinger.pictures";
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
        $message .= "<b>" . safeString($key) . "</b><br/>"
            . safeString($value) . "<br/>\n";
    }
    $message .= '</body></html>';
    if (empty($from) || empty($betreff) || !str_contains($from, "@")) {
        print_r($inputJSON);
    } else
        return mail($to, "[Kontaktformular] Neue Anfrage: " . $betreff, $message, $headers);
}

function safeString(string $string)
{
    return utf8_decode(htmlspecialchars($string, ENT_XML1));
}

?>
