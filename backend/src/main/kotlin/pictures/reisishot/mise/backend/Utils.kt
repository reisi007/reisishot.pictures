package pictures.reisishot.mise.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@ObsoleteCoroutinesApi
suspend fun <E> Iterable<E>.forEachLimitedParallel(callable: (E) -> Unit) = coroutineScope {
    map {
        launch(
            /*TODO, maybe Dispatchers IO is enough... newFixedThreadPoolContext(2 * Runtime.getRuntime().availableProcessors(), "Test")*/
            Dispatchers.IO
        ) { callable(it) }
    }
}

@ObsoleteCoroutinesApi
suspend fun <E> Iterable<E>.forEachParallel(callable: (E) -> Unit) = coroutineScope {
    map { launch { callable(it) } }
}