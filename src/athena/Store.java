package athena;
import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

public class Store<V> {
	public static final int MAX_SEED_SIZE = 500;

	ArrayList<Value<V>> values = new ArrayList<Value<V>>();

	private final int maxShortcuts;

	public Store(final int maxShortcuts) {
		this.maxShortcuts = maxShortcuts;
		// The first element is a dummy "seed" value which allows us
		// to skip into the store initially
		values.add(new Value<V>(null, null, MAX_SEED_SIZE, values.size()));
	}

	public void add(final Set<String> tags, final V value) {
		values.add(new Value<V>(tags, value, maxShortcuts, values.size()));
	}

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
				if (!e.getKey().match(values.get(e.getValue()).tags)) {
					System.out.println(e.getKey() + " doesn't match " + value
							+ " even though this is skipped to from position " + x);
					return false;
				}
			}
		}
		return true;
	}

	public StoreIterable<V> find(final Query q) {
		return new StoreIterable<V>(this, q);
	}

	public void resetShortcuts() {
		for (final Value<V> value : values) {
			value.shortcuts.clear();
		}
	}

	public int shortcutCount() {
		int ret = 0;
		for (final Value<V> value : values) {
			ret += value.shortcuts.size();
		}
		return ret;
	}

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


class Value<V> {
	public final Set<String> tags;
	public final V value;

	HashMap<Query, Integer> shortcuts;
	public final int position;

	public Value(final Set<String> tags, final V value, final int maxSize, final int position) {
		this.position = position;
		this.tags = tags;
		this.value = value;
		if (maxSize == Integer.MAX_VALUE) {
			shortcuts = Maps.newHashMap();
		} else {
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