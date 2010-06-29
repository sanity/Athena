package athena.benchmarks;

import java.text.DecimalFormat;
import java.util.*;

import org.testng.v6.Sets;

import com.google.common.collect.*;

import athena.*;
import athena.Query.*;
import athena.Store.StoreIterable;

public class ShortcutNumberBenchmark {
	public static final int QUERIES = 100, OBJECTS = 100000, TAGS = 50, TAGS_PER_OBJECT = 10, QUERY_MAX_DEPTH = 7;
	private static final int QUERY_CYCLES = 2;

	public static Query generateRandomQuery(final Random r, final int maxDepth, final ArrayList<String> availableTags) {
		if (maxDepth == 1)
			return new Contains(availableTags.get(r.nextInt(availableTags.size())));
		final int sel = r.nextInt(8);
		if (sel < 5)
			return new And(generateRandomQuery(r, maxDepth - 1, availableTags), generateRandomQuery(r, maxDepth - 1,
					availableTags));
		if (sel == 5)
			return new Or(generateRandomQuery(r, maxDepth - 1, availableTags), generateRandomQuery(r, maxDepth - 1,
					availableTags));
		if (sel == 6)
			return new Not(generateRandomQuery(r, maxDepth - 1, availableTags));
		if (sel == 7)
			return new Contains(availableTags.get(r.nextInt(availableTags.size())));
		throw new RuntimeException();
	}
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final Random r = new Random();
		final ArrayList<String> availableTags = Lists.newArrayList();
		System.out.print("Generating tags: ");
		for (int x = 0; x < TAGS; x++) {
			availableTags.add(Integer.toString(x));
		}
		final List<Set<String>> tags = Lists.newLinkedList();
		for (int x = 0; x < OBJECTS; x++) {
			final Set<String> t = Sets.newHashSet();
			for (int y = 0; y < TAGS_PER_OBJECT; y++) {
				t.add(availableTags.get(r.nextInt(availableTags.size())));
			}
			tags.add(t);
		}
		System.out.println("Done.");
		System.out.print("Generating queries: ");
		final List<Query> queries = Lists.newLinkedList();
		for (int x = 0; x < QUERIES; x++) {
			queries.add(generateRandomQuery(r, QUERY_MAX_DEPTH, availableTags));
		}
		System.out.println("Done.");
		System.out.println("shorts\tscanned\tResults (ttl)\ttests\tms");
		for (int x = 1; x < 100; x *= 2) {
			runTest(availableTags, tags, x - 1, queries);
		}
	}
	public static void runTest(final ArrayList<String> availableTags, final List<Set<String>> tags,
			final int maxShortcuts, final List<Query> queries) {
		final Object object = new Object();
		final Store<Object> s = new Store<Object>(maxShortcuts);
		for (final Set<String> t : tags) {
			s.add(t, object);
		}
		double ttlTests = 0, ttlResults = 0, ttlScanned = 0, count = 0;
		final long startTime = System.currentTimeMillis();
		for (int x = 0; x < QUERY_CYCLES; x++) {
			for (final Query q : queries) {
				final StoreIterable<Object> results = s.find(q);
				// if (!s.checkIntegrity())
				// throw new RuntimeException("Integrity check failed");
				ttlScanned += tags.size();
				ttlResults += Iterables.size(results);
				ttlTests += results.counter();
				count++;
			}
		}
		if (!s.checkIntegrity())
			throw new RuntimeException("Integrity check failed");
		final DecimalFormat df = new DecimalFormat("###,###,###.#");
		System.out.println(maxShortcuts + "\t" + df.format(ttlScanned / count) + "\t" + df.format(ttlResults / count)
				+ " (" + df.format(ttlResults)
				+ ")\t" + df.format(ttlTests / count) + "\t"
				+ df.format(((double) System.currentTimeMillis() - startTime) / count));
	}

}

