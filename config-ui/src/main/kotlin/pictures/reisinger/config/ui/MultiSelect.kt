package pictures.reisinger.config.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    val selectedSorted by remember(selectedItems) { mutableStateOf(selectedItems.sortedBy { it.toString() }) }
    val displayItems by remember(
        items,
        selectedItems,
        searchText
    ) { mutableStateOf((items - selectedItems).filterBySearchText(searchText)) }

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

        TextFieldWithSpellcheck(
            label = label,
            value = searchText,
            onDone = {
                if (searchText.isBlank()) return@TextFieldWithSpellcheck
                val item = createItem(searchText)
                setItemSelected(item)
                searchText = ""
            },
            onFocusChanged = { expanded = it.isFocused }
        ) { searchText = it }

        Box {
            DropdownMenu(
                expanded = expanded,
                focusable = false,
                onDismissRequest = {
                    expanded = false
                }) {
                displayItems.forEach {
                    DropdownMenuItem(onClick = {
                        expanded = false
                        searchText = ""
                        setItemSelected(it)
                    }) {
                        Text(it.toString())
                    }
                }
            }
        }
    }
}

private fun <T> List<T>.filterBySearchText(text: String): List<T> {
    if (text.isBlank())
        return this
    return filter { it.toString().contains(text, ignoreCase = true) }
}
