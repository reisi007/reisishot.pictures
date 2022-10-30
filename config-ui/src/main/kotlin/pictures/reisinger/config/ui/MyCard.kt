package pictures.reisinger.config.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.then(Modifier.padding(vertical = 8.dp)),
        backgroundColor = MaterialTheme.colors.background,
        elevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(4.dp), content = content)
    }
}
