<?php include '_cors.php'; ?>

<div>
    <?php
    $wage = 65; // Angepeilter Stundenlohn
    $session_fee_h = 0.5; // Fixkosten (Beratung / Vorbereitung Upload)
    $commercial = 0.35; // Aufpreis für kommerzielle Nutzung
    $noads = 0.10; // Aufpreis, wenn ich die Bilder nicht verwenden darf
    $nachbestellung = .9; // Aufpreis für Leistungen, die nicht im Paket inkludiert sind

    $pics = [
        "m" => 6,
        "l" => 3
    ];

    $level = [
        "m" => "erweiteter",
        "l" => "professioneller"
    ];

    function formatPrice($price)
    {
        return round($price);
    }

    $stunden = max(0, doubleval($_GET['stunden'] ?? '0'));
    $bilder = max(0, intval($_GET['bilder'] ?? '0'));
    $oeffentlich = boolval($_GET['nutzung'] ?? '0');
    $kommerziell = boolval($_GET['kommerziell'] ?? '0');
    $editLevel = ($_GET['editLevel'] ?? '')[0];

    if (!array_search($editLevel, array_keys($level)))
        $editLevel = 'm';
    ?>
    Kosten für das ausgewählte Paket (<?php echo $stunden; ?>h Shootingzeit /
    <?php echo $bilder . ' Bild' . ($bilder != 1 ? 'er' : ''); ?> mit <?php echo $level[$editLevel]; ?>
    Bearbeitung):<br/>
    <?php
    $price = $session_fee_h * $wage;

    // Bilder in Stunden umrechnen
    $pics1h = $pics[$editLevel];
    $stunden += $bilder / $pics1h;

    // Preis berechnen
    $price += $wage * $stunden;

    // % Aufpreise
    $fees = 1;
    if ($kommerziell)
        $fees += $commercial;
    if (!$oeffentlich)
        $fees += $noads;
    $price *= $fees;


    ?>
    <p><b style="font-size:1.5rem"><?php echo formatPrice($price) ?> €</b></p>
    <ul>
        <?php
        if ($oeffentlich)
            echo '<li><small>Der Fotograf darf die Bilder kommerziell verwenden</small></li>';

        if (!$kommerziell)
            echo '<li><small>Kommerzielle Nutzung (z.B. Verwendung auf Firmenwebseite) ist nicht gestattet!</small></li>';
        ?>
    </ul>
    <small>Weitere Bilder pro Stück:
        <?php
        foreach ($pics as $key => $value) {
            echo formatPrice($wage / $value * ($fees + $nachbestellung)) . ' €';
            echo ' inklusive ' . $level[$key] . ' Bearbeitung';
            if ($key !== array_key_last($pics))
                echo ' / ';
        } ?>
    </small><br/>
    <small>Je weiterer 15 Minuten Shootingzeit:
        <?php
        echo formatPrice($wage / 4 * ($fees + $nachbestellung));
        ?>
        €</small><br/>
    <small><a href="<?php
        echo 'https://' . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'];
        ?>" target="_blank">Angebot in neuem Tab öffnen</a></small>
</div>