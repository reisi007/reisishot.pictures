<?php include '_cors.php'; ?>
<p>
    <?php
    $session_fee = 24.99;
    $wage = 65;
    $commercial = 0.5;
    $private_noads = 0.1;
    $commercial_noads = 0.5;

    $pics1h = [
        "s" => 5,
        "m" => 3,
        "l" => 2
    ];

    $picsRest = [
        "s" => 15,
        "m" => 5,
        "l" => 3
    ];

    $level = [
        "s" => "einfacher",
        "m" => "erweiteter",
        "l" => "professioneller"
    ];

    $stunden = max(0, doubleval($_GET['stunden'] ?? '0'));
    $bilder = max(0, intval($_GET['bilder'] ?? '0'));
    $oeffentlich = boolval($_GET['nutzung'] ?? '0');
    $kommerziell = boolval($_GET['kommerziell'] ?? '0');
    $editLevel = ($_GET['editLevel'] ?? '')[0];

    if (!in_array($editLevel, array_keys($level))) {
        $editLevel = 'm';
    }

    if ($editLevel == 's') $oeffentlich = '0';

    ?>
    Kosten für das ausgewählte Paket (<?php echo $stunden; ?>h Shootingzeit /
    <?php echo $bilder . ' Bild' . ($bilder != 1 ? 'er' : ''); ?> mit <?php echo $level[$editLevel]; ?>
    Bearbeitung):<br/>
    <?php
    $price = 0;

    if ($bilder > 0 || $stunden > 0)
        $price += $session_fee;

    // Bilder in Stunden umrechnen
    if ($bilder > $pics1h[$editLevel])
        $stunden += 1 + ($bilder - $pics1h[$editLevel]) / $picsRest[$editLevel];
    else
        $stunden += $bilder / $pics1h[$editLevel];

    // Preis berechnen
    $price += $wage * $stunden;

    // % Aufpreise
    $fees = 1;
    if ($kommerziell)
        $fees += $commercial;
    if (!$oeffentlich)
        if ($kommerziell)
            $fees += $commercial_noads;
        else
            $fees += $private_noads;
    $price *= $fees;

    echo '<p><b style="font-size:1.5rem">' . round($price, 2) . ' €</b></p><ul>';

    if ($oeffentlich)
        echo '<li><small>Der Fotograf darf die Bilder kommerziell verwenden</small></li>';

    if (!$kommerziell)
        echo '<li><small>Kommerzielle Nutzung (z.B. Verwendung auf Firmenwebseite) ist nicht gestattet!</small></li>';

    echo '</ul>';
    ?>
</p>