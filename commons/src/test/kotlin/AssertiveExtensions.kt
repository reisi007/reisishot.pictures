package at.reisishot.mise.commons.testfixtures

import org.assertj.core.api.SoftAssertions

fun softAssert(block: SoftAssertions.() -> Unit) {
    SoftAssertions().apply(block).assertAll()
}
