package athena;
import java.util.*;

import com.google.common.collect.Sets;

/**
 * We could definitely be smarter about identifying all the falseIfFalse and
 * trueIfTrue queries
 * 
 * @author Ian Clarke <ian@sensearray.com>
 * 
 */
public abstract class Query {
	public static Not and(final Query a) {
		return new Not(a);
	}

	public static And and(final Query a, final Query b) {
		return new And(a, b);
	}

	public static And and(final Query a, final String b) {
		return new And(a, tag(b));
	}

	public static And and(final String a, final Query b) {
		return new And(tag(a), b);
	}

	public static And and(final String a, final String b) {
		return new And(tag(a), tag(b));
	}

	public static Not not(final Query a) {
		return new Not(a);
	}

	public static Not not(final String a) {
		return new Not(tag(a));
	}

	public static Or or(final Query a, final Query b) {
		return new Or(a, b);
	}

	public static Or or(final Query a, final String b) {
		return new Or(a, tag(b));
	}

	public static Or or(final String a, final Query b) {
		return new Or(tag(a), b);
	}

	public static Or or(final String a, final String b) {
		return new Or(tag(a), tag(b));
	}

	public static Contains tag(final String tag) {
		return new Contains(tag);
	}

	private final int hashCode;

	public static final QueryIntPair SHORTCUT_NOT_FOUND = new QueryIntPair(null, -1);

	public Query() {
		hashCode = toString().hashCode();
	}

	// TODO: Should reflect symmetry of OR and AND
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Query))
			return false;
		else if (((Query) obj).hashCode != hashCode)
			return false;
		else
			return obj.toString().equals(toString());
	}

	/**
	 * @return A non-exhaustive set of other queries that will not match if this
	 *         query doesn't match a given set of tags.
	 */
	public abstract Set<Query> falseIfFalse();

	public abstract QueryIntPair findShortCut(final HashMap<Query, Integer> shortcuts);

	// TODO: Should reflect symmetry of OR and AND
	@Override
	public int hashCode() {
		return hashCode;
	}

	public abstract boolean match(Set<String> tags);

	@Override
	public abstract String toString();

	/**
	 * 
	 * @return A non-exhaustive set of other queries that must match if this
	 *         query matches a given set of tags.
	 */
	public abstract Set<Query> trueIfTrue();

	public static class And extends Query {

		private final Query a;
		private final Query b;

		Set<Query> falseIfFalse, trueIfTrue;

		public And(final Query a, final Query b) {
			super();
			this.a = a;
			this.b = b;
			trueIfTrue = Sets.<Query> newHashSet(this);
			trueIfTrue.addAll(a.trueIfTrue());
			trueIfTrue.addAll(b.trueIfTrue());
			falseIfFalse = Collections.<Query> singleton(this);
		}
		@Override
		public Set<Query> falseIfFalse() {
			return falseIfFalse;
		}

		@Override
		public QueryIntPair findShortCut(final HashMap<Query, Integer> shortcuts) {
			final Integer ret = shortcuts.get(this);
			if (ret != null)
				return new QueryIntPair(this, ret);
			else {
				final QueryIntPair aQIP = a.findShortCut(shortcuts);
				final QueryIntPair bQIP = a.findShortCut(shortcuts);
				if (aQIP.i > bQIP.i)
					return aQIP;
				else
					return bQIP;
			}
		}


		@Override
		public boolean match(final Set<String> tags) {
			return a.match(tags) && b.match(tags);
		}

		@Override
		public String toString() {
			return "(" + a + " AND " + b + ")";
		}

		@Override
		public Set<Query> trueIfTrue() {
			return trueIfTrue;
		}

	}

	public static class Contains extends Query {
		private final String tag;

		Set<Query> falseIfFalse, trueIfTrue;

		public Contains(final String tag) {
			super();
			this.tag = tag;
			falseIfFalse = trueIfTrue = Collections.<Query> singleton(this);
		}

		@Override
		public Set<Query> falseIfFalse() {
			return Collections.<Query>singleton(this);
		}

		@Override
		public QueryIntPair findShortCut(final HashMap<Query, Integer> shortcuts) {
			final Integer ret = shortcuts.get(this);
			if (ret != null) return new QueryIntPair(this, ret);
			else return SHORTCUT_NOT_FOUND;
		}

		@Override
		public boolean match(final Set<String> tags) {
			return tags.contains(tag);
		}

		@Override
		public String toString() {
			return "\"" + tag + "\"";
		}

		@Override
		public Set<Query> trueIfTrue() {
			return Collections.<Query> singleton(this);
		}
	}

	public static class Not extends Query {
		private final Query a;

		Set<Query> falseIfFalse, trueIfTrue;

		public Not(final Query a) {
			super();
			this.a = a;
			falseIfFalse = trueIfTrue = Collections.<Query> singleton(this);
		}

		@Override
		public Set<Query> falseIfFalse() {
			return Collections.<Query> singleton(this);
		}

		@Override
		public QueryIntPair findShortCut(final HashMap<Query, Integer> shortcuts) {
			final Integer ret = shortcuts.get(this);
			if (ret != null)
				return new QueryIntPair(this, ret);
			else
				return SHORTCUT_NOT_FOUND;
		}

		@Override
		public boolean match(final Set<String> tags) {
			return !a.match(tags);
		}

		@Override
		public String toString() {
			return "(!" + a + ")";
		}

		@Override
		public Set<Query> trueIfTrue() {
			return Collections.<Query> singleton(this);
		}
	}

	public static class Or extends Query {

		private final Query a;

		private final Query b;

		Set<Query> falseIfFalse, trueIfTrue;


		public Or(final Query a, final Query b) {
			super();
			this.a = a;
			this.b = b;
			falseIfFalse = Sets.<Query> newHashSet(this);
			falseIfFalse.addAll(a.falseIfFalse());
			falseIfFalse.addAll(b.falseIfFalse());
			trueIfTrue = Collections.<Query> singleton(this);
		}

		@Override
		public Set<Query> falseIfFalse() {
			return falseIfFalse;
		}

		@Override
		public QueryIntPair findShortCut(final HashMap<Query, Integer> shortcuts) {
			final Integer ret = shortcuts.get(this);
			if (ret != null)
				return new QueryIntPair(this, ret);
			else {
				final QueryIntPair aQIP = a.findShortCut(shortcuts);
				final QueryIntPair bQIP = a.findShortCut(shortcuts);

				if (aQIP.i < bQIP.i)
					return aQIP;
				else
					return bQIP;
			}
		}

		@Override
		public boolean match(final Set<String> tags) {
			return a.match(tags) || b.match(tags);
		}

		@Override
		public String toString() {
			return "(" + a + " OR " + b + ")";
		}

		@Override
		public Set<Query> trueIfTrue() {
			return trueIfTrue;
		}


	}

	public static class QueryIntPair {
		public final int i;
		public final Query q;
		public QueryIntPair(final Query q, final int i) {
			this.q = q;
			this.i = i;
		}
	}
}
