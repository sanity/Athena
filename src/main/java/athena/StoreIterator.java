package athena;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Maps;

import athena.Query.QueryIntPair;

public class StoreIterator<V> extends AbstractIterator<V> {

	/**
	 * The Store that created this Iterator
	 */
	private final Store<V> parent;
	private final Query query;
	/**
	 * This indicates the position in the Store.values ArrayList that we are
	 * currently looking at
	 */
	private int position = 0;
	public static final boolean verbose = false;
	/**
	 * This keeps track of the last Value specific queries matched. We use it to
	 * retrospecively add shortcuts to those values when we find a new value
	 * that the query matches.
	 */
	private final Map<Query, Value<V>> previousMatchMap = Maps.newHashMap();
	private final Store.StoreIterable<V> iterable;

	public StoreIterator(final Store.StoreIterable<V> iterable, final Store<V> parent, final Query query) {
		this.iterable = iterable;
		iterable.counter = 0;
		this.parent = parent;
		this.query = query;
	}

	/**
	 * To understand what computeNext() does check the documentation for
	 * AbstractIterator in Google Collections
	 */
	@Override
	protected V computeNext() {
		// We loop until we have run out of Values
		while (position < parent.values.size()) {
			Value<V> current = null;
			/*
			 * Find the next position in Store.values that isn't null. This
			 * should only be necessary once we start supporting deletion.
			 */
			while (current == null) {
				current = parent.values.get(position);
				if (current == null) {
					position++;
				}
			}

			if (verbose) {
				System.out.println("+++++++++++++++++++++++");
				System.out.println("position: " + position + " examining " + current);
			}
			if (current.tags != null) { // Don't do this for the very first
				// "seed" value as it is a dummy
				iterable.counter++;

				// For every query in previousMatchMap, if it matches the
				// current
				// Value, then add a shortcut to this Value from the last Value
				// the query matched, and replace that Value in the
				// previosMatchMap
				// with the current value.
				for (final Entry<Query, Value<V>> e : previousMatchMap.entrySet()) {
					if (e.getKey().match(current.tags)) {
						// No point in a shortcut that only takes us to the
						// following value in the ArrayList
						if (position > e.getValue().position + 1) {
							e.getValue().shortcuts.put(e.getKey(), position);
						}
						previousMatchMap.put(e.getKey(), current);
					}
				}
			}

			// Add any queries to previousMatchMap that aren't already there and
			// are implied true by this query, they should point to the current
			// value
			for (final Query q : this.query.trueIfTrue()) {
				if (!previousMatchMap.containsKey(q)) {
					previousMatchMap.put(q, current);
				}
			}

			// Try to find a shortcut we can use to skip some values
			final QueryIntPair bestShortcut = query.findShortCut(current.shortcuts);

			if (bestShortcut.i != -1) {
				// Yay, we found a shortcut, now we must remove anything from
				// previousMatchMap that isn't implied false by bestShortcut
				// being false
				for (final Iterator<Query> it = previousMatchMap.keySet().iterator(); it.hasNext();) {
					if (!bestShortcut.q.falseIfFalse().contains(it.next())) {
						it.remove();
					}
				}
				// And finally update our position, skipping ahead
				position = bestShortcut.i;
			} else {
				// Can't find a shortcut, just move to the next position
				position++;
			}

			// If this current Value matches the query, return it
			if (current.tags != null && query.match(current.tags))
				return current.value;

			// The current value didn't match, continue the while() loop
		}

		// We've run out of values, point everything in previousMatchMap to
		// the position after the end of the ArrayList
		for (final Entry<Query, Value<V>> e : previousMatchMap.entrySet()) {
			e.getValue().shortcuts.put(e.getKey(), position);
		}

		return endOfData();
	}

}