package athena.benchmarks;

import java.util.*;

import org.testng.v6.Sets;

import com.google.common.collect.Iterables;

import athena.*;
import athena.Store.StoreIterable;

import static athena.Query.and;
import static athena.Query.or;

public class AdNetworkBenchmark {
	public static Random random = new Random();

	private static Object ad = new Object();

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		for (int shorts = 0; shorts < 30; shorts++) {
			System.out.print(shorts + "\t");
			runTest(5000, 100, 10, shorts, 5000);
		}
	}

	private static void runTest(final int adCount, final int locationCount, final int categoryCount,
			final int maxShortcuts, final int queries) {
		final Store<Object> store = new Store<Object>(maxShortcuts);
		for (int x=0; x<adCount; x++) {
			final Set<String> tags = Sets.newHashSet();
			tags.add("loc:" + random.nextInt(locationCount));
			tags.add("cat:" + random.nextInt(categoryCount));
			tags.add("adult:" + random.nextBoolean());
			store.add(tags, ad);
		}

		int resultCount = 0, testCount = 0;

		for (int x = 0; x < queries; x++) {
			final int queryType = random.nextInt(3);
			Query query;
			if (queryType == 0) {
				query = and("loc:" + random.nextInt(locationCount),
						and("cat:" + random.nextInt(categoryCount), "adult:" + random.nextBoolean()));
			} else if (queryType == 1) {
				query = and(or("loc:" + random.nextInt(locationCount), "loc:" + random.nextInt(locationCount)),
						and("cat:" + random.nextInt(categoryCount), "adult:" + random.nextBoolean()));
			} else {
				query = and(
						or("loc:" + random.nextInt(locationCount), "loc:" + random.nextInt(locationCount)),
						or(and("cat:" + random.nextInt(categoryCount), "adult:" + random.nextBoolean()),
								and("cat:" + random.nextInt(categoryCount), "adult:" + random.nextBoolean())));
			}
			final StoreIterable<Object> results = store.find(query);
			resultCount += Iterables.size(results);
			testCount += results.counter();
		}
		System.out.println(resultCount + "\t" + testCount + "\t"
				+ ((double) (testCount) / ((queries * adCount))));
	}
}
