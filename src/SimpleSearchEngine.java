import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class SimpleSearchEngine {

	private static final String bingSearchURL = "https://api.datamarket.azure.com/Bing/Search/Web?Query=%27";
	private static final String bingSearchFomart = "%27&$top=10&$format=json";
	private static String accountKey;
	private static String accountKeyEnc;
	private static final int numDoc = 10;
	private static final double alpha = 1;
	private static final double beta = 0.75;
	private static final double gamma = 0.15;
	private int relevantNum = 0;
	private int nonRelevantNum = 0;
	private int queryNum = 1;
	private double precision;
	public SimpleSearchEngine()
	{
		byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
		accountKeyEnc = new String(accountKeyBytes);
	}

	public static void main(String[] args)
	{
		if (args.length < 3)
		{
			System.out.println("not enough argument ");
			System.exit(-1);
		}
		//accountKey = args[0];
		accountKey ="hOVysMk4Ynb2GSI7COBxmjJf+GXpgKMP0xcy3RpYVY4";
		String queryTerm = args[2];
		SimpleSearchEngine inst = new SimpleSearchEngine();
		inst.precision = Double.parseDouble(args[1]);
		checkPrecisionAndUpdateQuery(inst, queryTerm);
	}

	private void precisionReached(String queryTerm, double precision)
	{
		System.out.println("Feedback Summary");
		System.out.println("query: " + queryTerm);
		System.out.println("precision: " + precision);
		System.out.println("desired precision reached done");
	}

	private static void checkPrecisionAndUpdateQuery(SimpleSearchEngine inst, String queryTerm)
	{
		boolean exit = false;
		do
		{
			JSONArray output = inst.getQUeryResult(queryTerm);
			if (output.size() < numDoc)
			{
				System.out.println("the initial result is less than 10. terminating");
				System.exit(-1);
			}
			Map<Integer, Boolean> relevantMap = inst.markRelevance(output);
			int countFalse = 0;
			for (Map.Entry<Integer, Boolean> entry : relevantMap.entrySet())
			{
				if (!entry.getValue())
				{
					countFalse++;
				}
				System.out.println("it is " + entry.getValue());
			}
			if (countFalse == numDoc)
			{
				System.out.println("it should terminate");
				System.exit(-1);
			}
			if (((double)(numDoc - countFalse)/numDoc) >= inst.precision)
			{
				inst.precisionReached(queryTerm, (double)(numDoc - countFalse)/numDoc);
				exit = true;
			}
			else
			{
				queryTerm = updateQueryTerm(inst, output, queryTerm, relevantMap);
			}
		}while(!exit);
	}

	private static String updateQueryTerm(SimpleSearchEngine inst, JSONArray output, String queryTerm, Map<Integer, Boolean> relevantMap)
	{
		Map<Integer, Map<String, Integer>> docTf = new HashMap<Integer, Map<String, Integer>>();
		Map<String, Integer> docIdf = new HashMap<String, Integer>();
		Set<String> dictionary = inst.calculateTfIDF(output, docIdf, docTf);
		List<String> vector = new ArrayList<String>(dictionary);

		Map<Integer, List<Double>> docVector = inst.calculateDocweight(vector,docTf, docIdf);

		List<Double> queryVector = inst.calculateQueryVector(vector, queryTerm);
		TreeMap<Integer, Double> updatedVector = inst.upDateQueryVector(queryVector, relevantMap, docVector);
		updatedVector = (TreeMap<Integer, Double>) sortByValues(updatedVector);
		int count = 0;
		String newQuery = "";
		for (Map.Entry<Integer, Double> temp : updatedVector.entrySet())
		{
			System.out.println("the weight is " + temp.getValue());
			System.out.println("the index is " + temp.getKey());
			System.out.println("the corresponding word is " + vector.get(temp.getKey()));
			newQuery +=  vector.get(temp.getKey());
			count++;
			if (count == 3)
			{
				break;
			}
			else
			{
				newQuery += "%20";
			}
		}
		return newQuery;
	}

	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
		Comparator<K> valueComparator =  new Comparator<K>() {
			public int compare(K k1, K k2) {
				int compare = map.get(k2).compareTo(map.get(k1));
				if (compare == 0) return 1;
				else return compare;
			}
		};
		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
		sortedByValues.putAll(map);
		return sortedByValues;
	}


	private TreeMap<Integer, Double> upDateQueryVector(List<Double> queryVector, Map<Integer, Boolean> relevantMap, Map<Integer, List<Double>> docVector)
	{
		TreeMap<Integer, Double> updatedQueryVector = new TreeMap<Integer,Double>();
		for (int i = 0; i < queryVector.size();i ++)
		{
			double term1 = SimpleSearchEngine.alpha*queryVector.get(i);
			double term2Sum = 0;
			double term3Sum = 0;
			for (Map.Entry<Integer, Boolean> relevance : relevantMap.entrySet())
			{
				if (relevance.getValue())
				{
					term2Sum += docVector.get(relevance.getKey()).get(i);
				}
				else
				{
					term3Sum += docVector.get(relevance.getKey()).get(i);
				}
			}
			double term2 = (SimpleSearchEngine.beta * term2Sum)/relevantNum;
			double term3 = (SimpleSearchEngine.gamma * term3Sum)/nonRelevantNum;
			updatedQueryVector.put(i, term1 + term2 - term3);
		}
		return updatedQueryVector;
	}


	private List<Double> calculateQueryVector(List<String> vector, String queryTerm)
	{
		String [] words = queryTerm.split("\\s+");
		List<String> wordList = new ArrayList<String>(Arrays.asList(words));
		List<Double> queryVector = new ArrayList<Double>();
		for (int i = 0; i < vector.size(); i++)
		{
			if (wordList.contains(vector.get(i)))
			{
				queryVector.add((double) 1);
			}
			else
			{
				queryVector.add((double) 0);
			}
		}
		return queryVector;
	}

	private Map<Integer, List<Double>> calculateDocweight(List<String> vector, Map<Integer, Map<String, Integer>> docTf, Map<String, Integer> idf)
	{
		Map<Integer, List<Double>> DocVecotr = new HashMap<Integer, List<Double>>();
		for (int j = 0; j < numDoc; j++)
		{
			List<Double> weightList = new ArrayList<Double>();
			for (int i = 0; i < vector.size(); i++)
			{
				double tf = docTf.get(j).get(vector.get(i)) == null ? 0 : docTf.get(j).get(vector.get(i));
				double weight = tf *Math.log(numDoc/idf.get(vector.get(i)));
				weightList.add(weight);
			}
			DocVecotr.put(j, weightList);
		}
		return DocVecotr;
	}

	private Set<String> calculateTfIDF(JSONArray result, Map<String, Integer> idf, Map<Integer, Map<String, Integer>> docTf)
	{
		Set<String> dictionary = new HashSet<String>();
		for (int i = 0;i < result.size(); i++)
		{
			Map<String, Integer> tf = new HashMap<String, Integer>();
			Set<String> wordOccurence = new HashSet<String>();
			JSONObject document = (JSONObject) result.get(i);
			String description = (String) document.get("Description");
			String title = (String) document.get("Title");
			String text = description +" " +  title;
			text = text.replaceAll("[^A-Za-z0-9 ]", "");
			text = text.toLowerCase();
			String[] arrayOfWords = text.split("\\s+");
			for (String tempWord : arrayOfWords)
			{
				if (!dictionary.contains(tempWord))
				{
					dictionary.add(tempWord);
				}
				if (!tf.containsKey(tempWord))
				{
					tf.put(tempWord, 1);
				}
				else
				{
					tf.put(tempWord, tf.get(tempWord) + 1) ;
				}
				if (!wordOccurence.contains(tempWord))
				{
					wordOccurence.add(tempWord);
					if (!idf.containsKey(tempWord))
					{
						idf.put(tempWord, 1);
					}
					else
					{
						idf.put(tempWord, idf.get(tempWord) + 1);
					}
				}
			}
			docTf.put(i, tf);
		}
		return dictionary;
	}

	private Map<Integer, Boolean> markRelevance (JSONArray result)
	{
		Map<Integer, Boolean> relevantMap = new HashMap<Integer, Boolean>();
		for (int i = 0; i < result.size(); i++)
		{
			JSONObject tempObj = (JSONObject)result.get(i);
			printInformation(tempObj, i, relevantMap);
		}
		return relevantMap;
	}


	private void printInformation(JSONObject obj, int index, Map<Integer, Boolean> relevantMap)
	{
		System.out.println("Result " + (index + 1));
		System.out.println("[");
		System.out.println("URL: " + obj.get("Url"));
		System.out.println("Title:  " + obj.get("Title"));
		System.out.println("Summary: " + obj.get("Description"));
		System.out.println("]");
		System.out.println("Relevant (Y/N)? ");
		do
		{
			Scanner sc = new Scanner(System.in);
			String userInput = sc.next();
			if (userInput.compareToIgnoreCase("Y") == 0)
			{
				relevantMap.put(index, true);
				relevantNum++;
				break;
			}
			else if (userInput.compareToIgnoreCase("N") == 0)
			{
				relevantMap.put(index, false);
				nonRelevantNum++;
				break;
			}
			else
			{
				System.out.println("valid input required");
			}
		}while(true);

	}

	private JSONArray getQUeryResult (String queryTerm)
	{
		System.out.println("query term is " + queryTerm);
		String URL = bingSearchURL + queryTerm + bingSearchFomart;
		URL url;
		JSONArray array = null;
		try {
			url = new URL(URL);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);
			InputStream inputStream = (InputStream) urlConnection.getContent();		
			byte[] contentRaw = new byte[urlConnection.getContentLength()];
			inputStream.read(contentRaw);
			String content = new String(contentRaw);
			JSONObject obj=(JSONObject) JSONValue.parse(content);
			JSONObject obj2 = (JSONObject) obj.get("d");
			array= (JSONArray) obj2.get("results");
		}
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return array;
	}

}
