package pictures.reisinger.config.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
fun <T : Displayable> MultiSelect(items: List<T>, selectedItems: Set<T>, setItemSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val selectedSorted by remember(selectedItems) { mutableStateOf(selectedItems.sortedBy { it.displayName }) }
    val displayItems by remember(items, selectedItems) { mutableStateOf(items - selectedItems) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        selectedSorted.forEach {
            Surface(
                modifier = Modifier.padding(4.dp),
                elevation = 8.dp,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.primary
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(it.displayName)
                    Icon(Icons.Default.Close, "Close ${it.displayName}")
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        OutlinedButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Add, "Select items")
        }

        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            displayItems.forEach {
                DropdownMenuItem(onClick = {
                    expanded = false
                    setItemSelected(it)
                }) {
                    Text(it.displayName)
                }
            }
        }
    }
}


interface Displayable {
    val displayName: String
}
