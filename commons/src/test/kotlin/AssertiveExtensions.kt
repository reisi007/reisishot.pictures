package at.reisishot.mise.commons.testfixtures

import org.assertj.core.api.AutoCloseableSoftAssertions
import org.assertj.core.api.SoftAssertions

fun softAssert(block: SoftAssertions.() -> Unit) {
    AutoCloseableSoftAssertions().use(block)
}
