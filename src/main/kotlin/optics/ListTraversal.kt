package optics

/**
 * Allows to focus on some list of items [I] (selected by [get]) from structure [S]
 */
class ListTraversal<S, I>(
    val get: (S) -> List<I>,
    val modify: (S, (I) -> I) -> S
) {
    fun set(s: S, item: I) = modify(s) { item }

    companion object {
        fun <I> source(predicate: (Int, I) -> Boolean): ListTraversal<List<I>, I> {
            return ListTraversal(
                get = { it.filterIndexed(predicate) },
                modify = { s, update ->
                    s.mapIndexed { index, item ->
                        if (predicate(index, item)) {
                            update(item)
                        } else {
                            item
                        }
                    }
                }
            )
        }

        fun <I> all(): ListTraversal<List<I>, I> = source { _, _ -> true }
        fun <I> each(predicate: (I) -> Boolean): ListTraversal<List<I>, I> = source { _, item -> predicate(item) }
        fun <I> eachPos(predicate: (Int) -> Boolean): ListTraversal<List<I>, I> = source { ind, _ -> predicate(ind) }
        fun <I> at(position: Int): ListTraversal<List<I>, I> = source { ind, _ -> ind == position }
    }
}

// Composition

infix fun <A, B, C> ListTraversal<A, B>.at(traversal: ListTraversal<B, C>): ListTraversal<A, C> {
    return ListTraversal(
        get = { a ->
            this.get(a).flatMap { item ->
                traversal.get(item)
            }
        },
        modify = { a, update ->
            this.modify(a) { b ->
                traversal.modify(b, update)
            }
        }
    )
}

infix fun <A, B, C> ListTraversal<A, B>.at(lens: Lens<B, C>) = this at lens.toListTraversal()
infix fun <A, B, C> ListTraversal<A, B>.at(optLens: OptLens<B, C>) = this at optLens.toListTraversal()
