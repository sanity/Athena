package athena;
import java.util.*;
import java.util.Map.Entry;

import athena.Query.QueryIntPair;

import com.google.common.collect.*;

/*
 * (c) Copyright 2008 Uprizer Labs Llc
 * All rights reserved
 * 
 * This source code is a trade secret of Uprizer Labs Llc.
 */

public class StoreIterator<V> extends AbstractIterator<V> {

	private final Store<V> parent;
	private final Query query;
	private int position = 0;
	public static final boolean verbose = false;
	private final Map<Query, Value<V>> previousMatchMap = Maps.newHashMap();
	private final Store.StoreIterable<V> iterable;

	public StoreIterator(final Store.StoreIterable<V> iterable, final Store<V> parent, final Query query) {
		this.iterable = iterable;
		iterable.counter = 0;
		this.parent = parent;
		this.query = query;
		// *** I think this should be implied
		// for (final Query q : query.trueIfTrue()) {
		// // Initially we use the seed Value as the last match
		// previousMatchMap.put(q, parent.values.get(0));
		// }
	}

	@Override
	protected V computeNext() {
		while (position < parent.values.size()) {
			Value<V> current = null;
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
			boolean matched = false;
			if (current.tags != null) { // Don't do this for the seed value
				iterable.counter++;
				matched = query.match(current.tags);
				for (final Entry<Query, Value<V>> e : previousMatchMap.entrySet()) {
					if (e.getKey().match(current.tags)) {
						if (position > e.getValue().position + 1) {
							e.getValue().shortcuts.put(e.getKey(), position);
						}
						previousMatchMap.put(e.getKey(), current);
					}
				}
			}

			// Add any queries to previousMatchMap that aren't already there and
			// are implied true by this query, they should point to the current
			// value TODO: Think about whether this is a good idea
			for (final Query q : this.query.trueIfTrue()) {
				if (!previousMatchMap.containsKey(q)) {
					previousMatchMap.put(q, current);
				}
			}

			// Ok, let's find the next valid match if we can
			final QueryIntPair bestShortcut = query.findShortCut(current.shortcuts);

			if (bestShortcut.i != -1) {
				// Remove anything from previousMatchMap that isn't implied false by
				// bestShortcut being false
				for (final Iterator<Query> it = previousMatchMap.keySet().iterator(); it.hasNext();) {
					if (!bestShortcut.q.falseIfFalse().contains(it.next())) {
						it.remove();
					}
				}
				position = bestShortcut.i;
			} else {
				position++;
			}

			if (matched)
				return current.value;
		}

		// We've run out of values, point everything to one after the last one
		for (final Entry<Query, Value<V>> e : previousMatchMap.entrySet()) {
			e.getValue().shortcuts.put(e.getKey(), position);
		}

		return endOfData();
	}

}