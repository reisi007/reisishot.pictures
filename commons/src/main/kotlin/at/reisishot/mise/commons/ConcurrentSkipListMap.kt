package at.reisishot.mise.commons

import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListMap

fun <K, V> concurrentSkipListMap(comparator: Comparator<in K>): ConcurrentMap<K, V> =
    ConcurrentSkipListMap(comparator)

fun <K : Comparable<K>, V> concurrentSkipListMap(): ConcurrentMap<K, V> = ConcurrentSkipListMap()
