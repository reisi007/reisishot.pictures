<?php include '_cors.php'; ?>
<div>
    <?php
    $wage = 70; // Angepeilter Stundenlohn
    $session_fee_h = 1; // Fixkosten (Beratung / Vorbereitung Upload)
    $album_fee_h = 0.5; // Fixkosten Erstellung Fotobuch
    $commercial = 1 / 3; // Aufpreis kommerzielle Nutzung
    $nachbestellung_bilder = 2.4; // Aufpreis für Leistungen, die nicht im Paket inkludiert sind
    $nachbestellung_stunden = 0.5; // Aufpreis für Leistungen, die nicht im Paket inkludiert sind
    $nachbestellung_alben = 1; // Aufpreis für Leistungen, die nicht im Paket inkludiert sind

    $pics = [
        "m" => 8,
        "l" => 4
    ];
    // $base,$includedPages,$perPage, $shipping,$discount

    $albums = [
        'q14' => [9.99, 0, 0.1, 10, "Gute Qualität circa 14x14, 16 Seiten (Ringbindung)"], // Fotoheft 14x14
        'qA5' => [29.99, 0, 0.1, 20, "Hohe Qualität, 26 Seiten circa A5 hoch , A5 quer oder 15x15"], // Saal Digital Fotobuch 15x21 bzw. 15x15
        'qA4' => [29.99, 0, 0.1, 35, "Hohe Qualität, 26 Seiten circa A4 hoch, A4 quern oder 19x19"], // Saal Digital 28 x 28
        'q28' => [39.99, 0, 0.1, 30, "Hohe Qualität, 26 Seiten circa 28x28"], // Saal Digital 28 x 28
        'l21' => [64.99, 0, 0.1, 40, "Luxoriöse Qualität, 26 Seiten circa 21x21"], // Professional Line 21x21
        'l21b' => [104.99, 0, 0.1, 50, "Luxoriöse Qualität, 26 Seiten circa 21x21 inklusive Box"], // Professional Line 21x21
        'lA4' => [74.99, 0, 0.1, 40, "Luxoriöse Qualität, 26 Seiten circa A4 hoch oder A4 quer"], // Professional Line 22x30 / 30x21
        'lA4b' => [119.99, 0, 0.1, 50, "Luxoriöse Qualität, 26 Seiten circa A4 hoch oder A4 quer inklusive Box"], // Professional Line 22x30 / 30x21
        'l30' => [89.99, 0, 0.1, 40, "Luxoriöse Qualität, 26 Seiten circa 30x30"], // Professional Line 30x30
        'l30b' => [139.99, 0, 0.1, 50, "Luxoriöse Qualität, 26 Seiten circa 30x30 inklusive Box"], // Professional Line 30x30
    ];

    $level = [
        "m" => "erweiteter",
        "l" => "professioneller"
    ];

    $stunden = max(0, doubleval($_GET['stunden'] ?? '0'));
    $bilder = max(0, intval($_GET['bilder'] ?? '0'));
    $kommerziell = boolval($_GET['kommerziell'] ?? '0');
    $editLevel = ($_GET['editLevel'] ?? '')[0];
    $album = ($_GET['album'] ?? '');
    $standalone = boolval($_GET['standalone'] ?? '0');
    $rabatt_stunden = max(0.0, doubleval($_GET['rh'] ?? '0'));
    $rabatt_prozent = max(0.0, doubleval($_GET['rp'] ?? '0')) / 100.0;

    // % Aufpreise
    $fees = 1;
    if ($kommerziell)
        $fees += $commercial;

    $rabatt = $rabatt_stunden * $fees * $wage * (1 - $rabatt_prozent);


    function formatPricePercent($price, $rabatt)
    {
        if ($rabatt == 0)
            formatPriceEur($price, 0);
        else
            formatPriceEur($price, $price * $rabatt);
    }

    function formatPrice($price)
    {
        return ceil($price) . ' €';
    }

    function formatPriceEur($price, $rabatt)
    {
        if ($rabatt == 0)
            echo formatPrice($price);
        else
            echo '<s>' . formatPrice($price) . '</s> <b>' . formatPrice($price - $rabatt) . '</b>';

    }

    function album_price($albums, $album, $rabatt)
    {
        if ($album == '')
            return 0;

        $a = $albums[$album];
        //Fotobuch Preis berechnen [Preis, Versandkosten, Rabatt, Aufschlag, Beschreibung]
        return $a[0] * (1 - $a[2]) + ($a[3] * (1 - $rabatt)) + $a[1];
    }

    if (!array_search($editLevel, array_keys($level)))
        $editLevel = 'm';

    if (!in_array($album, array_keys($albums)))
        $album = '';

    if ($standalone) {
        ?>
        Kosten für das ausgewählte Paket (<?php echo $stunden; ?>h Shootingzeit /
        <?php echo $bilder . ' Bild' . ($bilder != 1 ? 'er' : ''); ?> mit <?php echo $level[$editLevel]; ?>
        Bearbeitung):<br/>
        <?php
        if ($rabatt_prozent > 0 || $rabatt_stunden > 0) {
            $rabattEur = $rabatt_stunden * $fees * $wage;
            echo 'Rabatt in der Höhe von ';
            if ($rabatt_stunden > 0)
                echo formatPrice($rabattEur) . ' ';
            if ($rabatt_prozent > 0 && $rabatt_stunden > 0)
                echo 'und anschließend noch ';
            if ($rabatt_prozent)
                echo 'circa ' . $rabatt_prozent * 100 . '% Rabatt auf die Dienstleistungen';

        }
    }

    $price = 0;

    // Bilder in Stunden umrechnen
    $pics1h = $pics[$editLevel];
    $stunden += $session_fee_h;
    $stunden += ($bilder / $pics1h);

    $aPrice = album_price($albums, $album, 0);
    $rabatt += $aPrice - album_price($albums, $album, $rabatt_prozent);

    if ($album != '')
        $stunden += $album_fee_h;


    // Preis berechnen
    $hPreis = $wage * $stunden;
    if ($rabatt_prozent > 0)
        $rabatt += $hPreis * $rabatt_prozent;
    $price += $hPreis;

    $price *= $fees;

    $price += $aPrice;
    ?>
    <p><b style="font-size:1.5rem"><?php formatPriceEur($price, $rabatt); ?></b></p>
    <ul>
        <?php
        if ($album != "")
            echo '<li><small>Inklusive Album "' . $albums[$album][4] . '"</small><br/></li>';

        if (!$kommerziell)
            echo '<li><small>Kommerzielle Nutzung (z.B. Verwendung auf Firmenwebseite) ist nicht gestattet!</small></li>';
        ?>
    </ul>
    <small>Weitere Bilder pro Stück:
        <?php
        $pp = 0;
        foreach ($pics as $key => $value) {
            $cp = $wage / $value * ($fees + $nachbestellung_bilder);
            formatPricePercent($cp, $rabatt_prozent);

            if ($pp > 0) {
                echo ' (Upgrade ';
                formatPricePercent($cp - $pp, $rabatt_prozent);
            }
            echo ' inklusive ' . $level[$key] . ' Bearbeitung';
            if ($key !== array_key_last($pics))
                echo ' / ';
            $pp = $cp;
        } ?>
    </small><br/>
    <small>Je weiterer 15 Minuten Shootingzeit:
        <?php formatPricePercent($wage / 4 * ($fees + $nachbestellung_stunden), $rabatt_prozent); ?>
    </small><br/>
    <small><br/>
        Weitere Alben:
        <ul>
            <?php
            foreach (array_keys($albums) as $ak) {
                ?>
                <li><small><?php
                        echo $albums[$ak][4] . ': ';
                        $af = ($fees + $nachbestellung_alben);
                        $fullPrice = album_price($albums, $ak, 0) * $af;
                        $aR = album_price($albums, $ak, $rabatt_prozent) * $af;
                        formatPriceEur($fullPrice, $fullPrice - $aR);
                        ?></small></li>
                <?php
            }
            ?>

        </ul>
    </small>
    <?php
    if (!boolval($standalone)) { ?>
        <small>
            <a href="<?php
            echo 'https://' . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'] . '&standalone=1';
            ?>" target="_blank">Angebot in neuem Tab öffnen</a>
        </small>
    <?php } ?>
</div>
