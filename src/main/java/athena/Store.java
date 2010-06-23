package athena;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;

/**
 * Stores values, each of which is associated with a set of tags, and allows
 * retrieval of those tags using a boolean query.
 * 
 * @author Ian Clarke <ian@sensearray.com>
 * 
 * @param <V>
 *            The type of the values stored
 */
public class Store<V> {
	public static final int MAX_SEED_SIZE = 500;

	/**
	 * This is where the actual data is stored.
	 */
	protected ArrayList<Value<V>> values = new ArrayList<Value<V>>();

	private final int maxShortcuts;

	/**
	 * Create a store
	 * 
	 * @param maxShortcuts
	 *            The maximum number of shortcuts permitted per-value. Higher
	 *            means faster searches but higher memory usage. Lower means the
	 *            opposite.
	 */
	public Store(final int maxShortcuts) {
		this.maxShortcuts = maxShortcuts;
		// The first element is a dummy "seed" value which allows us
		// to skip into the store initially
		values.add(new Value<V>(null, null, MAX_SEED_SIZE, values.size()));
	}

	/**
	 * Add a value to the Store, associating it with a Set of tags
	 */
	public void add(final Set<String> tags, final V value) {
		values.add(new Value<V>(tags, value, maxShortcuts, values.size()));
	}

	/**
	 * A debugging utility method that will verify that shortcuts never skip
	 * over matching values, and always point to values that do match the
	 * associated query.
	 * 
	 * @return True if all is well, false if there is a problem
	 */
	public boolean checkIntegrity() {
		for (int x = 1; x < values.size(); x++) {
			final Value<V> value = values.get(x);
			for (final Entry<Query, Integer> e : value.shortcuts.entrySet()) {
				for (int y=x+1; y<e.getValue(); y++) {
					if (e.getKey().match(values.get(y).tags)) {
						System.err.println(e.getKey() + " matches " + value
								+ " even though this is skipped from position " + x);
						return false;
					}
				}
				if (e.getValue() < values.size() && !e.getKey().match(values.get(e.getValue()).tags)) {
					System.out.println(e.getKey() + " doesn't match " + value
							+ " even though this is skipped to from position " + x);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Find the values whose tags match a query
	 * 
	 * @param q
	 *            The query to match
	 * @return An Iterable of 0 or more search results. Note that these are
	 *         returned "lazily" from the Iterable.
	 */
	public StoreIterable<V> find(final Query q) {
		return new StoreIterable<V>(this, q);
	}

	/**
	 * Removes all shortcuts from the store. The search that follows this will
	 * require time proportional to the size of the store.
	 */
	public void resetShortcuts() {
		for (final Value<V> value : values) {
			value.shortcuts.clear();
		}
	}

	/**
	 * Counts the total number of shortcuts in the store, returning the result.
	 */
	public int shortcutCount() {
		int ret = 0;
		for (final Value<V> value : values) {
			ret += value.shortcuts.size();
		}
		return ret;
	}

	/**
	 * An Iterable that serves as a simple wrapper around StoreIterator.
	 */
	public static class StoreIterable<V> implements Iterable<V> {

		private final Store<V> parent;
		private final Query query;
		int counter = 0;

		protected StoreIterable(final Store<V> parent, final Query query) {
			this.parent = parent;
			this.query = query;

		}

		public int counter() {
			return counter;
		}

		@Override
		public Iterator<V> iterator() {
			return new StoreIterator<V>(this, parent, query);
		}

	}
}

/**
 * A wrapper around a value, which contains associated information about the
 * value. Most notibly it contains shortcuts to future values which are key to
 * Athena's efficiency.
 * 
 * @author Ian Clarke <ian@sensearray.com>
 * 
 * @param <V>
 */
class Value<V> {
	/**
	 * The tags associated with this value
	 */
	public final Set<String> tags;

	/**
	 * The value itself.
	 */
	public final V value;

	/**
	 * Shortcuts to future values. A Query -> Integer entry in this Map
	 * indicates that the next Value which matches this Query is at position
	 * Integer. This allows StoreIterator to avoid having to check every single
	 * value in Store.values.
	 */
	HashMap<Query, Integer> shortcuts;

	/**
	 * The position of this value in Store.values
	 */
	// TODO: Would be nice to find a way to get rid of this
	public final int position;

	protected Value(final Set<String> tags, final V value, final int maxSize, final int position) {
		this.position = position;
		this.tags = tags;
		this.value = value;
		if (maxSize == Integer.MAX_VALUE) {
			// No limit on size, just use a normal HashMap for shortcuts
			shortcuts = Maps.newHashMap();
		} else {
			// Use a LRU hashmap to restrict the number of shortcuts
			shortcuts = new LinkedHashMap<Query, Integer>(16, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(final java.util.Map.Entry<Query, Integer> eldest) {
					return size() > maxSize;
				}
			};
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Value [position=");
		builder.append(position);
		builder.append(", tags=");
		builder.append(tags);
		builder.append(", shortcuts=");
		builder.append(shortcuts);
		builder.append("]");
		return builder.toString();
	}


}