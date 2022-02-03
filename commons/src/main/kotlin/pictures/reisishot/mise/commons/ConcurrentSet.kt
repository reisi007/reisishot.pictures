@file:Suppress("unused")

package pictures.reisishot.mise.commons

import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun <T> concurrentSetOf(): ConcurrentSet<T> = Collections.newSetFromMap(ConcurrentHashMap())
fun <T> concurrentSetOf(vararg elements: T): ConcurrentSet<T> = concurrentSetOf<T>().apply { addAll(elements) }
fun <T> concurrentSetOf(elements: Iterable<T>): ConcurrentSet<T> = concurrentSetOf<T>().apply { addAll(elements) }
fun <T> concurrentSetOf(element: T): ConcurrentSet<T> = concurrentSetOf<T>().apply { add(element) }

typealias ConcurrentSet<T> = MutableSet<T>
