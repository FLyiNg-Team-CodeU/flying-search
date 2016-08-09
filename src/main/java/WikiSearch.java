import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import joptsimple.internal.Strings;
import redis.clients.jedis.Jedis;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
		this.map = map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 *
	 */
	private  void print() {
		List<Entry<String, Integer>> entries = sort();
		for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
        // TODO FILL THIS IN!
		Map<String, Integer> union = new HashMap<String, Integer>(map);
		for (String key: that.map.keySet()) {
			int relevance = totalRelevance(this.getRelevance(key), that.getRelevance(key));
			union.put(key, relevance);
		}
		return new WikiSearch(union);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
        // TODO FILL THIS IN!
		Map<String, Integer> intersection = new HashMap<String, Integer>();
		for (String key: map.keySet()) {
			if (that.map.containsKey(key)) {
				int relevance = totalRelevance(map.get(key), that.map.get(key));
				intersection.put(key, relevance);
			}
		}
		return new WikiSearch(intersection);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
        // TODO FILL THIS IN!
		Map<String, Integer> difference = new HashMap<String, Integer>(map);
		for (String term: that.map.keySet()) {
			difference.remove(term);
		}
		return new WikiSearch(difference);
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort() {
        // TODO FILL THIS IN!
		// make a list of entries
		List<Entry<String, Integer>> entries =
				new LinkedList<Entry<String, Integer>>(map.entrySet());

		// make a Comparator object for sorting
		Comparator<Entry<String, Integer>> comparator = new Comparator<Entry<String, Integer>>() {

			public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
				return e1.getValue().compareTo(e2.getValue());
			}
		};

		// sort and return the entries
		Collections.sort(entries, comparator);
		return entries;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCounts(term);
		return new WikiSearch(map);
	}

    private static List<String> splitArgs(List<String> args, String delimiter) {
        List<String> argsBefore = new ArrayList<String>();
        for (String arg: args) {
            if (arg.equals("or")) {
                break;
            }
            argsBefore.add(arg);
        }
        return argsBefore;
    }

    private static WikiSearch evaluateAnd(List<String> args, JedisIndex index) {
        if (args.size() == 1) {
            return search(args.get(0), index);
        }
        List<String> argsBefore = splitArgs(args, "and");
        List<String> argsAfter = new ArrayList<String>(args.subList(argsBefore.size(), args.size()));
        WikiSearch before = evaluateMinus(argsBefore, index);
        WikiSearch after = evaluateAnd(argsAfter, index);
        return before.and(after);
    }

    private static WikiSearch evaluateMinus(List<String> args, JedisIndex index) {
        if (args.size() == 1) {
            return search(args.get(0), index);
        }
        List<String> argsBefore = splitArgs(args, "minus");
        List<String> argsAfter = new ArrayList<String>(args.subList(argsBefore.size(), args.size()));
        WikiSearch before = evaluateMinus(argsBefore, index);
        WikiSearch after = evaluateMinus(argsAfter, index);
        return before.minus(after);
    }

    private static WikiSearch evaluateOr(List<String> args, JedisIndex index){
        if (args.size() == 1) {
            return search(args.get(0), index);
        }
        List<String> argsBefore = splitArgs(args, "or");
        List<String> argsAfter = new ArrayList<String>(args.subList(argsBefore.size(), args.size()));
        WikiSearch before = evaluateAnd(argsBefore, index);
        WikiSearch after = evaluateOr(argsAfter, index);
        return before.or(after);
    }

	public static void main(String[] args) throws IOException {
		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);

        OptionParser parser = new OptionParser();
        parser.accepts("keyword").withRequiredArg().ofType(String.class);
//        parser.accepts("a", "and").withOptionalArg().ofType(String.class);
//        parser.accepts("o", "or").withOptionalArg().ofType(String.class);
//        parser.accepts("m", "minus").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(args);

//        List<List<?>> arguments = new ArrayList<List<?>>();
        List<String> arguments = (List<String>) options.valuesOf("keyword");
        WikiSearch searchResult = evaluateOr(arguments, index);
        searchResult.print();
		
//		// search for the first term
//		String term1 = "java";
//		System.out.println("Query: " + term1);
//		WikiSearch search1 = search(term1, index);
//		search1.print();
//
//		// search for the second term
//		String term2 = "programming";
//		System.out.println("Query: " + term2);
//		WikiSearch search2 = search(term2, index);
//		search2.print();
//
//		// compute the intersection of the searches
//		System.out.println("Query: " + term1 + " AND " + term2);
//		WikiSearch intersection = search1.and(search2);
//		intersection.print();
	}
}
