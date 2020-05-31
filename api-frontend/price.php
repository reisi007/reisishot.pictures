<?php include '_cors.php'; ?>
<div>
    <?php
    $inflation = 1;
    $wage_h = 70 * $inflation; // Angepeilter Stundenlohn
    $session_fee_duration = 1; // Fixkosten (Beratung / Vorbereitung Upload)
    $album_preparation_duration = 0; // Fixkosten Erstellung Fotobuch
    $fee_commercial_usage = 1 / 3; // Aufpreis kommerzielle Nutzung
    $nopackage_images = 1.5; // Aufpreis für Leistungen, die nicht im Paket inkludiert sind
    $nopackage_wage = 0.5; // Aufpreis für Leistungen, die nicht im Paket inkludiert sind
    $nopackage_album = 1; // Aufpreis für Leistungen, die nicht im Paket inkludiert sind

    $edits_per_hour = [
        "m" => 8,
        "l" => 4
    ];

    // $base,$includedPages,$perPage, $shipping,$discount
    class Album
    {
        private $product_price;
        private $shipping_costs;
        private $discount;
        private $margin;
        private $description;

        /**
         * Album constructor.
         * @param $product_price
         * @param $shipping_costs
         * @param $discount
         * @param $margin
         * @param $description
         */
        public function __construct($product_price, $shipping_costs, $discount, $margin, $description)
        {
            $this->product_price = $product_price;
            $this->shipping_costs = $shipping_costs;
            $this->discount = $discount;
            $this->margin = $margin;
            $this->description = $description;
        }

        /**
         * @return mixed
         */
        public function getProductPrice()
        {
            return $this->product_price;
        }

        /**
         * @return mixed
         */
        public function getShippingCosts()
        {
            return $this->shipping_costs;
        }

        /**
         * @return mixed
         */
        public function getDiscount()
        {
            return $this->discount;
        }

        /**
         * @return mixed
         */
        public function getMargin()
        {
            return $this->margin;
        }

        /**
         * @return mixed
         */
        public function getDescription()
        {
            return $this->description;
        }


    }

    $available_albums = [
        'q14' => new Album(9.99, 0, 0.1, 10, "Gute Qualität circa 14x14, 16 Seiten (Ringbindung)"), // Fotoheft 14x14
        'qA5' => new Album(29.99, 0, 0.1, 30, "Hohe Qualität, 26 Seiten circa A5 hoch , A5 quer oder 15x15"), // Saal Digital Fotobuch 15x21 bzw. 15x15
        'qA4' => new Album(29.99, 0, 0.1, 45, "Hohe Qualität, 26 Seiten circa A4 hoch, A4 quern oder 19x19"), // Saal Digital 28 x 28
        'q28' => new Album(39.99, 0, 0.1, 45, "Hohe Qualität, 26 Seiten circa 28x28"), // Saal Digital 28 x 28
        'l21' => new Album(64.99, 0, 0.1, 60, "Luxoriöse Qualität, 26 Seiten circa 21x21"), // Professional Line 21x21
        'l21b' => new Album(104.99, 0, 0.1, 70, "Luxoriöse Qualität, 26 Seiten circa 21x21 inklusive Box"), // Professional Line 21x21
        'lA4' => new Album(74.99, 0, 0.1, 60, "Luxoriöse Qualität, 26 Seiten circa A4 hoch oder A4 quer"), // Professional Line 22x30 / 30x21
        'lA4b' => new Album(119.99, 0, 0.1, 70, "Luxoriöse Qualität, 26 Seiten circa A4 hoch oder A4 quer inklusive Box"), // Professional Line 22x30 / 30x21
        'l30' => new Album(89.99, 0, 0.1, 70, "Luxoriöse Qualität, 26 Seiten circa 30x30"), // Professional Line 30x30
        'l30b' => new Album(139.99, 0, 0.1, 80, "Luxoriöse Qualität, 26 Seiten circa 30x30 inklusive Box"), // Professional Line 30x30
    ];

    $edit_level_name = [
        "m" => "erweiteter",
        "l" => "professioneller"
    ];

    $package_hours = max(0, doubleval($_GET['stunden'] ?? '0'));
    $package_images = max(0, intval($_GET['bilder'] ?? '0'));
    $is_kommerziell = boolval($_GET['kommerziell'] ?? '0');
    $edit_level = ($_GET['editLevel'] ?? '')[0];
    $package_album = ($_GET['album'] ?? '');
    $standalone = boolval($_GET['standalone'] ?? '0');
    $rabatt_hours = max(0.0, doubleval($_GET['rh'] ?? '0'));
    $rabatt_service = max(0.0, doubleval($_GET['rp'] ?? '0')) / 100.0;

    // Calculate Fee amount
    $fees = 1;
    if ($is_kommerziell)
        $fees += $fee_commercial_usage;

    $rabatt_total = $rabatt_hours * $fees * $wage_h * (1 - $rabatt_service);


    function format_price_percent($price, $percent)
    {
        if ($percent <= 0)
            format_price_eur($price, 0);
        else
            format_price_eur($price, $price * $percent);
    }

    function format_price($price)
    {
        return ceil($price) . ' €';
    }

    function format_price_eur($price, $amount)
    {
        $price_full = format_price($price);
        $price_final = format_price($price - $amount);
        if ($price_full == $price_final)
            echo $price_full;
        else {
            echo '<b>' . $price_final . '</b> <small><s>' . $price_full . '</s></small>';
        }

    }

    function album_price($albums, $album, $percent)
    {
        if ($album == '')
            return 0;

        $a = $albums[$album];

        if (!($a instanceof Album)) {
            return 0;
        }
        return $a->getProductPrice() * (1 - $a->getDiscount()) + ($a->getMargin() * (1 - $percent)) + $a->getShippingCosts();
    }

    if (!array_search($edit_level, array_keys($edit_level_name)))
        $edit_level = 'm';

    if (!in_array($package_album, array_keys($available_albums)))
        $package_album = '';

    if ($standalone) {
        ?>
        Kosten für das ausgewählte Paket (<?php echo $package_hours; ?>h Shootingzeit / bis zu
        <?php echo $package_images . ' Bild' . ($package_images != 1 ? 'er' : ''); ?> mit <?php echo $edit_level_name[$edit_level]; ?>
        Bearbeitung):<br/>
        <?php
        if ($rabatt_service > 0 || $rabatt_hours > 0) {
            echo '<small>';
            $rabattEur = $rabatt_hours * $fees * $wage_h;
            echo 'Rabatt in der Höhe von ';
            if ($rabatt_hours > 0)
                echo format_price($rabattEur) . ' ';
            if ($rabatt_service > 0 && $rabatt_hours > 0)
                echo 'und zusätzlich noch ';
            if ($rabatt_service)
                echo 'circa ' . $rabatt_service * 100 . '% Rabatt auf die Dienstleistungen';
            echo '</small>';
        }
    }

    $price = 0;

    // Bilder in Stunden umrechnen
    $pics1h = $edits_per_hour[$edit_level];
    $package_hours += $session_fee_duration;
    $package_hours += ($package_images / $pics1h);

    $aPrice = album_price($available_albums, $package_album, 0);
    $rabatt_total += $aPrice - album_price($available_albums, $package_album, $rabatt_service);

    if ($package_album != '')
        $package_hours += $album_preparation_duration;


    // Preis berechnen
    $hPreis = $wage_h * $package_hours * $fees;
    if ($rabatt_service > 0)
        $rabatt_total += $hPreis * $rabatt_service;
    $price += $hPreis;

    $price += $aPrice;
    ?>
    <p><span style="font-size:1.5rem"><?php format_price_eur($price, $rabatt_total); ?></span> <small>sofort fällig:
            <i><?php
                echo format_price(max($wage_h, $price / 2));
                ?>
            </i></small></p>
    <ul>
        <?php
        if ($package_album != "")
            echo '<li><small>Inklusive Album "' . $available_albums[$package_album]->getDescription() . '"</small><br/></li>';

        if (!$is_kommerziell)
            echo '<li><small>Kommerzielle Nutzung (z.B. Verwendung auf Firmenwebseite) ist nicht gestattet!</small></li>';
        ?>
    </ul>
    <small>Weitere Bilder pro Stück:
        <?php
        $pp = 0;
        foreach ($edits_per_hour as $key => $value) {
            $cp = $wage_h / $value * ($fees + $nopackage_images);
            format_price_percent($cp, $rabatt_service);

            if ($pp > 0) {
                echo ' (Upgrade ';
                format_price_percent($cp - $pp, $rabatt_service);
                echo ') ';
            }
            echo ' inklusive ' . $edit_level_name[$key] . ' Bearbeitung';
            if ($key !== array_key_last($edits_per_hour))
                echo ' / ';
            $pp = $cp;
        } ?>
    </small><br/>
    <br/>
    <small>Je weiterer 15 Minuten Shootingzeit:
        <?php format_price_percent($wage_h / 4 * ($fees + $nopackage_wage), $rabatt_service); ?>
    </small><br/>
    <small><br/>
        Weitere Alben:
        <ul>
            <?php
            foreach (array_keys($available_albums) as $ak) {
                ?>
                <li><small><?php
                        echo $available_albums[$ak]->getDescription() . ': ';
                        $af = ($fees + $nopackage_album);
                        $fullPrice = album_price($available_albums, $ak, 0) * $af;
                        $aR = album_price($available_albums, $ak, $rabatt_service) * $af;
                        format_price_eur($fullPrice, $fullPrice - $aR);
                        ?></small></li>
                <?php
            }
            ?>

        </ul>
    </small>
    <?php
    if (!$standalone) { ?>
        <small>
            <a href="<?php
            echo 'https://' . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'] . '&standalone=1';
            ?>" target="_blank">Angebot in neuem Tab öffnen</a>
        </small>
    <?php } ?>
</div>
