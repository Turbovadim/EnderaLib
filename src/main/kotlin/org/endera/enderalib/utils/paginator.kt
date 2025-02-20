package org.endera.enderalib.utils

/**
 * Splits the original list into a list of lists, each of a fixed size, effectively paginating the original list.
 *
 * @param T The type of elements in this list.
 * @param pageSize The size of each page; determines how many elements each sublist will contain, except possibly the last one.
 * @return A list of lists, where each list represents a page containing up to [pageSize] elements of the original list.
 * If the original list is empty or [pageSize] is less than 1, the result will be an empty list.
 *
 * Example:
 * ```
 * val list = listOf(1, 2, 3, 4, 5)
 * val paginatedList = list.paginate(2) // Result: [[1, 2], [3, 4], [5]]
 * ```
 */
@Suppress("unused")
fun <T> List<T>.paginate(pageSize: Int): List<List<T>> = this.chunked(pageSize)