package pictures.reisishot.mise.backend.html

import kotlinx.html.*
import kotlin.math.max

@HtmlTagMarker
fun FlowContent.buildForm(title: DIV.() -> Unit, formStructure: () -> FormRoot, thankYouText: DIV.() -> Unit) {
    div("formContainer") {
        formStructure().let { root ->
            title()
            buildForm(root)

            div {
                attributes["r-form-submitted"] = root.name
                style = "display: none"
                thankYouText()
            }
        }
    }
}

private fun FlowContent.buildForm(cur: FormElement): Unit = when (cur) {
    is FormRoot -> form(classes = "needs-validation") {
        attributes["r-form"] = cur.name
        novalidate = true
        cur.builderElements.forEach { buildForm(it) }
        div {
            button(classes = "btn btn-primary") {
                text("Absenden")
            }
        }
    }
    is FormInput -> formElement(cur) {
        input(cur.type, classes = "form-control") {
            cur.defaultValue?.let { value = it }
            id = cur.name
            name = cur.name
            required = cur.required
            if (cur.placeholder != null)
                placeholder = cur.placeholder
        }
    }
    is HiddenFormInput -> input(InputType.hidden, classes = "form-control") {
        id = cur.name
        name = cur.name
        required = cur.required
        value = cur.value

    }
    is FormSelect -> formElement(cur) {
        select("form-control") {
            required = cur.required
            id = cur.name
            name = cur.name
            cur.options.forEach { option ->
                option {
                    attributes["value"] = option.value
                    text(option.label)
                }
            }
        }
    }
    is FormTextArea -> formElement(cur) {
        textArea(cur.rows.toString(), classes = "control") {
            required = cur.required
            name = cur.name
            id = cur.name
        }
    }
    is FormCheckbox -> div("form-check") {
        val helpId = cur.name + "Help"
        val description = cur.description

        input(type = InputType.checkBox) {
            required = cur.required
            name = cur.name
            id = cur.name
            value = ""
        }

        label(classes = "form-check-label") {
            attributes["for"] = cur.name
            attributes["aria-describedby"] = helpId
            text(cur.label)
        }

        cur.errorMessage?.let { errorMessage -> div("invalid-feedback") { text(errorMessage) } }
        p {
            small {
                id = helpId
                text(description)
            }
        }
    }
    is FormHGroup -> div("form-row") {
        cur.builderElements.forEach { child ->
            buildForm(child)
        }
    }
    is FormGroup -> throw IllegalStateException("There should not be any instances of FormGroup")
    is FormHtml -> div {
        cur.html(this)
    }
    is FormElement -> throw  IllegalStateException("There should not be any instances of FormElement")
}

private fun FlowContent.formElement(cur: FormBuilderElement, block: DIV.() -> Unit) = div("form-group") {
    val helpId = cur.name + "Help"
    val description = cur.description
    val label = cur.label
    label {
        attributes["for"] = cur.name
        if (description != null)
            attributes["aria-describedby"] = helpId
        label?.let {
            text(it)
            span {
                style = "color: red;"
                text(" *")
            }
        }
    }
    block()
    cur.errorMessage?.let { errorMessage -> div("invalid-feedback") { text(errorMessage) } }

    if (description != null)
        small {
            id = helpId
            text(description)
        }
}

sealed class FormElement

open class FormBuilderElement(
    val name: String,
    open val label: String? = null,
    open val description: String? = null,
    open val errorMessage: String? = null,
    val required: Boolean = true
) : FormElement()

class FormRoot(
    formName: String = "form",
    vararg val builderElements: FormBuilderElement
) : FormBuilderElement(formName)

class FormHGroup(
    vararg builderElements: FormBuilderElement
) : FormGroup(*builderElements)

abstract class FormGroup(
    vararg val builderElements: FormBuilderElement
) : FormBuilderElement("group")

class FormInput(
    name: String,
    override val label: String,
    override val description: String,
    errorMessage: String,
    val type: InputType,
    val placeholder: String? = null,
    val defaultValue: String? = null,
    required: Boolean = true
) : FormBuilderElement(name, label, description, errorMessage, required)

class HiddenFormInput(
    name: String,
    val value: String
) : FormBuilderElement(name, null, null, null, false)

class FormSelect(
    name: String,
    label: String,
    description: String,
    errorMessage: String,
    required: Boolean = true,
    vararg val options: FormSelectOption
) : FormBuilderElement(name, label, description, errorMessage, required)

data class FormSelectOption(
    val value: String,
    val label: String = value
)

class FormTextArea(
    name: String,
    label: String,
    description: String,
    errorMessage: String,
    rows: Int = 6,
    required: Boolean = true
) : FormBuilderElement(name, label, description, errorMessage, required) {
    val rows = max(1, rows)
}

class FormCheckbox(
    name: String,
    override val label: String,
    override val description: String,
    errorMessage: String
) : FormBuilderElement(name, label, description, errorMessage)

class FormHtml(
    val html: (DIV) -> Unit
) : FormElement()
