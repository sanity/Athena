package athena;
import java.util.*;

import com.google.common.collect.*;

import static athena.Query.*;

public class Test1 {
	private static Store<Set<String>> store;

	public static void main(final String[] args) {

		final Query qA = and("1", and(and(and("3", "2"), not("6")), not(and(not("3"), and("1", "6")))));

		final Query qB = and("3", and(and(and("3", "2"), not("6")), not(and(not("3"), and("1", "4")))));

		store = new Store<Set<String>>(2);
		final Random r = new Random(0);
		for (int x = 0; x <= 10000; x++) {
			final Set<String> tagSet = Sets.newHashSet();

			for (int y = 0; y < 100; y++) {
				if (r.nextDouble() < 0.1) {
					tagSet.add(""+y);
				}
			}
			store.add(tagSet, tagSet);
		}

		search(qA);
		search(qA);
		search(qB);
		search(qB);
		// for (int t = 0; t < store.values.size(); t++) {
		// System.out.println(t + " : " + store.values.get(t));
		// }
	}

	public static void search(final Query q) {
		final Store.StoreIterable<Set<String>> results4 = store.find(q);
		System.out.println("Query: " + q + ", Found " + Iterables.size(results4) + " results with "
				+ results4.counter() + " tests, shortcut count: " + store.shortcutCount());
		if (!store.checkIntegrity())
			throw new RuntimeException("Store integrity compromized :-(((");
	}
}