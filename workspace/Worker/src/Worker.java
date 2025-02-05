import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Worker extends Thread {

	private final BlockingQueue<String> workQueue;
	private final ConcurrentHashMap<String, String> dict;

	public Worker(ConcurrentHashMap<String, String> dict, BlockingQueue<String> workQueue) {
		super();
		this.workQueue = workQueue;
		this.dict = dict;
	}

	@Override
	public void run() {
		super.run();
		while (!Thread.currentThread().isInterrupted()) {
			try {
				String item = String.valueOf(workQueue.take());
				
				if (this.dict.containsKey(item)){
					continue;
				}

				System.out.println(item);
				String url = "http://www.thesaurus.com/browse/" + URLEncoder.encode(item);

				HttpClient.Response response = HttpClient.get(url);
				// System.out.println("Response has " +
				// response.getResponse().length());
				String dt = response.getResponse();

				String pattern = "<a href=\"http://www.thesaurus.com/browse/";
				int lastIndex = dt.indexOf(pattern, 0);
				int count = 5;
				while (lastIndex > -1 && count > 0) {
					count--;
					
					int endOfWord = dt.indexOf("\"", dt.indexOf("\"", lastIndex) + 1);
					if (endOfWord == -1)
						break;

					String word = dt.substring(lastIndex, endOfWord)
							.replace("<a href=\"http://www.thesaurus.com/browse/", "");
					word = URLDecoder.decode(word);
					int slashIndex = word.indexOf("/");
					if (slashIndex != -1)
						word = word.substring(0, slashIndex);

					if (!dict.contains(word)) {
						dict.put(word, word);
						System.out.println(word);
						workQueue.put(word);
						System.out.println(workQueue.size());
					}

					lastIndex = dt.indexOf(pattern, lastIndex + 1);
					Thread.sleep(500);
				}

				// Process item
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
