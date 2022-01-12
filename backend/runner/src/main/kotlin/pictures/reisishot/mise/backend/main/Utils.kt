package pictures.reisishot.mise.backend.main

import pictures.reisishot.mise.backend.FacebookMessengerChatPlugin
import pictures.reisishot.mise.backend.html.FormCheckbox

fun generateDefaultChatPlugin(): FacebookMessengerChatPlugin = FacebookMessengerChatPlugin(
    628453067544931,
    "#27ae60",
    "Hallo! Was kann ich für dich tun?"
)

val zustimmung = FormCheckbox(
    "Zustimmung",
    "Ich akzeptiere, dass der Inhalt dieses Formulars per Mail an den Fotografen zugestellt wird",
    "Natürlich wird diese E-Mail-Adresse nur zum Zwecke deiner Anfrage verwendet und nicht mit Dritten geteilt",
    "Leider benötige ich deine Einwilligung, damit du mir eine Nachricht schicken darfst"
)
