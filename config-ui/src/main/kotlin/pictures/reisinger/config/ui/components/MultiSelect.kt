package pictures.reisinger.config.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> MultiSelectWithSpellcheck(
    label: String,
    items: List<T>,
    createItem: (String) -> T,
    setItemSelected: (T) -> Unit,
    setItemsUnselected: (T) -> Unit,
    selectedItems: Set<T>,
    modifier: Modifier = Modifier
) {

    var searchText by remember { mutableStateOf("") }
    var expanded by remember(searchText) { mutableStateOf(searchText.isNotBlank()) }

    val selectedSorted by remember(selectedItems) { mutableStateOf(selectedItems.sortedBy { it.toString() }) }
    val displayItems by remember(
        items,
        selectedItems,
        searchText
    ) { mutableStateOf((items - selectedItems).filterBySearchText(searchText)) }
    var isDropdownInFocus by remember(expanded) { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        selectedSorted.forEach {
            Surface(
                modifier = Modifier.padding(4.dp).clickable { setItemsUnselected(it) },
                elevation = 8.dp,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.primary
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(it.toString())
                    Icon(Icons.Default.Close, "Close $it")
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        val focusRequester = remember { FocusRequester() }

        Column {
            TextFieldWithSpellcheck(
                label = label,
                value = searchText,
                onDone = {
                    if (searchText.isBlank()) return@TextFieldWithSpellcheck
                    val item = createItem(searchText)
                    searchText = ""
                    setItemSelected(item)
                },
                onDown = {
                    isDropdownInFocus = true
                    expanded = true
                }
            ) { searchText = it }
            Row {
                val onSelection: (item: T) -> Unit = {
                    expanded = false
                    searchText = ""
                    setItemSelected(it)
                }

                if (isDropdownInFocus)
                    DropdownMenu(
                        modifier = Modifier.heightIn(max = 500.dp).onFocusChanged {
                            if (it.isFocused)
                                focusRequester.requestFocus()
                        },
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        DropdownItems(displayItems, onSelection, focusRequester)
                    }
                else
                    DropdownMenu(
                        modifier = Modifier.heightIn(max = 500.dp),
                        expanded = expanded,
                        focusable = false,
                        onDismissRequest = { expanded = false }) {
                        DropdownItems(displayItems, onSelection, focusRequester)
                    }
            }
        }
    }
}

@Composable
private fun <T> DropdownItems(displayItems: List<T>, onSelection: (T) -> Unit, focusRequester: FocusRequester) {
    displayItems.forEachIndexed { idx, item ->
        DropdownMenuItem(
            modifier = if (idx == 1) Modifier.focusRequester(focusRequester) else Modifier,
            onClick = { onSelection(item) }
        ) {
            Text(item.toString())
        }
    }
}

private fun <T> List<T>.filterBySearchText(text: String): List<T> {
    if (text.isBlank())
        return this
    return filter { it.toString().startsWith(text, ignoreCase = true) }
}
