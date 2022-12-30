package pictures.reisinger.next

import pictures.reisishot.mise.backend.config.category.buildCategoryConfig
import pictures.reisishot.mise.backend.config.category.buildExcludeMatcher
import pictures.reisishot.mise.backend.config.category.buildIncludeSubdirectoriesMatcher
import pictures.reisishot.mise.backend.config.category.buildIncludeTagsMatcher
import pictures.reisishot.mise.backend.config.category.complexMatchAnd
import pictures.reisishot.mise.backend.config.category.computable.CameraLensCategoryComputable
import pictures.reisishot.mise.backend.config.category.computable.DateCategoryComputable
import pictures.reisishot.mise.backend.config.category.includeSubcategories
import pictures.reisishot.mise.backend.config.category.includeTagsAndSubcategories
import pictures.reisishot.mise.backend.config.category.matchOr
import pictures.reisishot.mise.backend.config.category.withComputedSubCategories
import pictures.reisishot.mise.backend.config.category.withSubCategory
import pictures.reisishot.mise.backend.config.tags.additionalTags
import pictures.reisishot.mise.backend.config.tags.buildTagConfig
import pictures.reisishot.mise.backend.config.tags.computable.computeTagsFromExif

object PrivateConfig {

    val TAG_CONFIG = buildTagConfig {

        computeTagsFromExif()

        additionalTags {

            "Frau" to arrayOf(
                "Sophie Huber",
                "Eva Mair",
                "Jessica Hettich",
                "Julia Eder",
                "Laura Schmiedinger",
                "Magdalena Wöhrer",
                "Johanna Kartusch",
                "Magdalena Adam",
                "Mintha",
                "Anja Stiftinger",
                "Victoria Lasinger",
                "Laura Szabo",
                "Lisa Matschl",
                "Iris Hammer",
                "Sarah Fritsch",
                "Michelle Kohlbauer",
                "Chrisi Music",
                "Susanne Zopf",
                "Sandra F"
            )

            "Mann" to arrayOf(
                "Simon Luger"
            )

            "Style in Motion" to arrayOf(
                "JC Crew",
                "Synergy Crew"
            )

            "Tiere" to arrayOf(
                "Eichhörnchen"
            )

            "Zoo" to arrayOf(
                "Tiergarten Schoenbrunn",
                "Aqua-Terra Zoo Wien",
                "BÄRENPARK Arbersbach",
                "Welser Tiergarten"
            )

            "Tanz" to arrayOf(
                "Style in Motion",
                "Breakdance"
            )

            "Simon Luger" withTags arrayOf("Back to Saturday")

            "Steel Wings Linz" withTags "Eishockey"

            "Teichralle" withTags "Vogel"

            "Motorsport" withTags "Sport"

            "Eidechse" withTags "Tier"
        }
    }

    val CATEGORY_CONFIG = buildCategoryConfig {
        withComputedSubCategories("Chronologisch") { DateCategoryComputable(it) }
        withComputedSubCategories("Ausrüstung") {
            CameraLensCategoryComputable(it) {
                mapOf(
                    Pair(null, null) to "Eichhörnchen002",
                    Pair("Canon EOS M50", null) to "Wien009"
                )
            }
        }

        withSubCategory("Natur", "Schmetterling023") {
            includeTagsAndSubcategories("Natur")

            withSubCategory("Tiere") {
                includeTagsAndSubcategories("Tiere")

                withSubCategory("Zoo") {
                    includeTagsAndSubcategories("Zoo")
                }

                withSubCategory("Hund") {
                    includeTagsAndSubcategories("Hund")
                }

                withSubCategory("Insekten") {
                    includeTagsAndSubcategories("Insekten")

                    withSubCategory("Wollschweber") {
                        includeTagsAndSubcategories("Wollschweber")
                    }

                    withSubCategory("Bienen") {
                        includeTagsAndSubcategories("Biene")
                    }

                    withSubCategory("Grashüpfer") {
                        includeTagsAndSubcategories("Grashuepfer")
                    }
                    withSubCategory("Schmetterling") {
                        includeTagsAndSubcategories("Schmetterling")
                    }
                }

                withSubCategory("Vögel") {
                    includeTagsAndSubcategories("Vogel")
                }

                withSubCategory("Katzen") {
                    includeTagsAndSubcategories("Katze")
                }
            }

            withSubCategory("Blumen") {
                includeTagsAndSubcategories("Blume")

                withSubCategory("Wassertropfen") {
                    includeTagsAndSubcategories("Wassertropfen")
                }
            }
        }

        withSubCategory("Detailaufnahmen", "Schwebfliege003") {
            includeTagsAndSubcategories("Detailaufnahme")
        }

        withSubCategory("Schwarz-Weiß", "Tier014") {
            includeTagsAndSubcategories("Schwarz Weiss")
        }

        withSubCategory("Nacht") {
            includeTagsAndSubcategories("Nacht")

            withSubCategory("Feuerwerk") {
                includeTagsAndSubcategories("Feuerwerk")
            }
            withSubCategory("Urfahranermarkt") {
                includeTagsAndSubcategories("Urfahranermarkt")
            }
        }

        withSubCategory("Österreich", "OperWien02") {

            complexMatchAnd(
                matchOr(
                    buildIncludeSubdirectoriesMatcher(),
                    buildIncludeTagsMatcher("Österreich"),
                ),
                buildExcludeMatcher("Natur")
            )

            withSubCategory("Baden bei Wien") {
                includeTagsAndSubcategories("Baden bei Wien")
            }
            withSubCategory("Ebensee") {
                includeTagsAndSubcategories("Ebensee")
            }
            withSubCategory("Traunkirchen") {
                includeTagsAndSubcategories("Traunkirchen")
            }
            withSubCategory("St. Pölten") {
                includeTagsAndSubcategories("St. Pölten")
            }
            withSubCategory("Graz") {
                includeTagsAndSubcategories("Graz")
            }
            withSubCategory("Krems") {
                includeTagsAndSubcategories("Krems")
            }
            withSubCategory("Salzburg") {
                includeTagsAndSubcategories("Salzburg")
            }
            withSubCategory("Semmering") {
                includeTagsAndSubcategories("Semmering")
            }
            withSubCategory("Ötscher") {
                includeTagsAndSubcategories("Ötschergräben")
            }
            withSubCategory("Wien") {
                includeTagsAndSubcategories("Wien")
            }
        }

        withSubCategory("Reisen") {
            includeSubcategories()

            withSubCategory("Zypern", "2019-Zypern30") {
                includeTagsAndSubcategories("Zypern")
            }
            withSubCategory("Prag") {
                includeTagsAndSubcategories("Prag")
            }
            withSubCategory("Bratislava") {
                includeTagsAndSubcategories("Bratislava")
            }
        }

        withSubCategory("Fotoshootings", "Frau33") {
            includeSubcategories()

            withSubCategory("Frauen", "SophieHuber016") {
                includeTagsAndSubcategories("Frau")
            }
            withSubCategory("Männer") {
                includeTagsAndSubcategories("Mann")
            }
            withSubCategory("Paare") {
                includeTagsAndSubcategories("Paare")
            }
            withSubCategory("Boudoir", "Boudoir0001") {
                includeTagsAndSubcategories("Boudoir")
            }
            withSubCategory("Musiker") {
                includeTagsAndSubcategories("Musiker")

                withSubCategory("Back to Saturday") {
                    includeTagsAndSubcategories("Back to Saturday")
                }
            }

            withSubCategory("Sport", "SteelWings0034") {
                includeTagsAndSubcategories("Sport")

                withSubCategory("Beachvolleyball") {
                    includeTagsAndSubcategories("Beachvolleyball")
                }

                withSubCategory("Floorball") {
                    includeTagsAndSubcategories("Floorball")
                }

                withSubCategory("Eishockey") {
                    includeTagsAndSubcategories("Eishockey")
                }

                withSubCategory("Tanz") {
                    includeTagsAndSubcategories("Tanz")

                    withSubCategory("Ballett") {
                        includeTagsAndSubcategories("Ballett")
                    }

                    withSubCategory("Breaking") {
                        includeTagsAndSubcategories("Breakdance")
                    }
                }

                withSubCategory("Flunkyball") {
                    includeTagsAndSubcategories("Flunkyball")
                }

                withSubCategory("Pole - Aerial Silk - Hoop"){
                    includeSubcategories()

                    withSubCategory("Pole"){
                        includeTagsAndSubcategories("Poledance")
                    }

                    withSubCategory("Aerial Silk"){
                        includeTagsAndSubcategories("Aerial Silk")
                    }

                    withSubCategory("Hoop"){
                        includeTagsAndSubcategories("Hoop")
                    }
                }
            }
        }
    }
}
