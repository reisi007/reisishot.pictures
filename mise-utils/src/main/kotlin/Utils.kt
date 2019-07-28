import java.nio.file.Path

val Path.filenameWithoutExtension: String
    get() = with(fileName.toString()) {
        substring(0, lastIndexOf('.'))
    }

inline fun <T> Sequence<T>.peek(crossinline peekingAction: (T) -> Unit) =
    map {
        peekingAction(it)
        it
    }