package athena;

import static athena.Query.and;
import static athena.Query.not;

import java.util.Random;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class IntegrityTests {
	Store<Set<String>> store;

	public void search(final Query q) {
		final Store.StoreIterable<Set<String>> results4 = store.find(q);
		System.out.println("Query: " + q + ", Found " + Iterables.size(results4) + " results with "
				+ results4.counter() + " tests, shortcut count: " + store.shortcutCount());
		Assert.assertTrue(store.checkIntegrity());
	}

	@Test
	public void simpleIntegrityTest() {
		final Query qA = and("1", and(and(and("3", "2"), not("6")), not(and(not("3"), and("1", "6")))));

		final Query qB = and("3", and(and(and("3", "2"), not("6")), not(and(not("3"), and("1", "4")))));

		// In this store, the values stored are the tags associated with them. A
		// bit weird I know, but I thought it might be useful for debugging.
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
}