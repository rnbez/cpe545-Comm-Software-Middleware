import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

	final static String CALL_EXAMPLE = "\nServer Usage:\n" + "    java Main [type] [ip address]\n" + "Example	:\n"
			+ "    java Main -server 127.0.0.1\n" + "\nClient Usage:\n"
			+ "    java Main [type] [policy file] [host:port]\n" + "Example:\n"
			+ "    java Main -client ..\\client.policy 127.0.0.1\n";
	final static String BINDING_NAME = "MY___SERVER";

	public static void main(String[] args) throws InterruptedException, UnknownHostException {
		// TODO Auto-generated method stub
		// final BlockingQueue<String> workQueue = new
		// LinkedBlockingQueue<String>();
		// final ConcurrentHashMap<String, String> dict = new
		// ConcurrentHashMap<>();
		// workQueue.put("cat");
		// workQueue.put("dog");
		// workQueue.put("grape");
		// Producer producer = new Producer(workQueue);
		// ArrayList<Consumer> consumers = new ArrayList<>();
		//
		// // producer.start();
		// for (int i = 0; i < 5; i++) {
		// Consumer c = new Consumer(dict, workQueue);
		// c.start();
		// consumers.add(c);
		// }
		//
		// Thread.sleep(10000000);
		//
		// System.out.println("Interrupting...");
		// producer.interrupt();
		// for (Consumer consumer : consumers) {
		// consumer.interrupt();
		// }

		if (!validateArgs(args)) {
			return;
		}

		String type = args[0];
		if (type.equals("-server")) {
			startServer(args);
		} else if (type.equals("-client")) {
			startClient(args);
		}

	}

	private static boolean validateArgs(String[] args) {
		if (args == null || args.length < 1) {
			System.out.println("Incorrect call.\n" + CALL_EXAMPLE);
			return false;
		}

		String type = args[0];
		if (type.equals("-server")) {
			int expectedNumberOfArgs = 2;
			if (args != null && args.length >= expectedNumberOfArgs) {
				String host = args[1];
				try {
					Inet4Address.getByName(host);
					return true;
				} catch (UnknownHostException e) {
					// e.printStackTrace();
					System.out.println("Invalid argument. You must provide a valid IP Address.");
				}
			} else {
				System.out.println("Incorrect call. " + "Expected " + expectedNumberOfArgs + " arguments.");
			}
			System.out.println(CALL_EXAMPLE);
			return false;
		} else if (type.equals("-client")) {
			int expectedNumberOfArgs = 3;
			if (args != null && args.length >= expectedNumberOfArgs) {
				String policy = args[1];
				String host = args[2];
				if (!new java.io.File(policy).exists()) {
					System.out.println("Invalid argument. File " + policy + " could not be found.");
				} else {
					try {
						Inet4Address.getByName(host);
						return true;
					} catch (UnknownHostException e) {
						// e.printStackTrace();
						System.out.println("Invalid argument. You must provide a valid IP Address.");
					}
				}
			} else {
				System.out.println("Incorrect call. " + "Expected " + expectedNumberOfArgs + " arguments.");
			}
			System.out.println(CALL_EXAMPLE);
			return false;
		} else {
			return false;
		}
	}

	public static void startServer(String[] args) throws UnknownHostException {
		final ArrayList<Thread> workerList = new ArrayList<>();
		final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(500);
		final ConcurrentHashMap<String, String> dict = new ConcurrentHashMap<>();
		final String bindName = BINDING_NAME;
		final String hostname = args[1];
		final int nworkers = Integer.valueOf(args[2]);
		final String startWord = args[3];

		try {
			System.setProperty("java.rmi.server.hostname", hostname);
			System.out.println("Hostname set to: " + hostname);
			System.out.println("Bind name set to: " + bindName);

			queue.put(startWord);
			RMIConnector conn = new RMIConnector(queue, dict, bindName);

			System.out.println("Object binded to name: " + bindName);
			System.out.println("Ready for connections...\n\n");

			System.out.println("Creating workers...");
			System.out.println(nworkers + " worker(s) will be created");
			
			Runnable runn = new Runnable() {

				@Override
				public void run() {
					String item = "";
					while (!Thread.currentThread().isInterrupted()) {
						try {
							item = queue.take();

							if (item.isEmpty() || dict.containsKey(item)) {
								continue;
							}

							dict.put(item, item);
							String url = "http://www.thesaurus.com/browse/" + URLEncoder.encode(item);

							System.out.println(item);
							// System.out.println("get " + url);
							HttpClient.Response response = HttpClient.get(url);
							// System.out.println("Response has " +
							// response.getResponse().length());
							String dt = response.getResponse();

							String pattern = "<a href=\"http://www.thesaurus.com/browse/";
							int lastIndex = dt.indexOf(pattern, 0);
							int count = 10;
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

								if (!dict.containsKey(word)) {
									queue.put(word);
								}

								lastIndex = dt.indexOf(pattern, lastIndex + 1);

								Thread.sleep(500);
							}

							// Process item
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
							break;
						} catch (IOException e) {
							System.err.println("Error catch when trying to process word \""+item+"\"");
//							e.printStackTrace();
						}
					}

				}
			};

			for (int i = 0; i < nworkers; i++) {
				// workerList.add(new Worker(dict, queue));
				workerList.add(new Thread(runn));
			}

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.out.print("Asking clients to stop...");
					try {
						conn.interruptWorkers();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.out.print(" Failed.");
					}
					System.out.println(" Done.");

					interruptWorkers(workerList);
				}
			});

			System.out.println("Starting workers...");
			for (Thread worker : workerList) {
				worker.start();
			}

			// while(true){
			// queue.put("cat");
			// }

			for (Thread worker : workerList) {
				worker.join();
			}

		} catch (Exception e) {
			System.out.println("MAIN:server:err: " + e.getMessage());
			e.printStackTrace();
			interruptWorkers(workerList);
		}
	}

	public static void startClient(String[] args) {
		final ArrayList<Thread> workerList = new ArrayList<>();
//		final BlockingQueue<String> queue;
//		final ConcurrentHashMap<String, String> dict;
		final String securityPolicy = args[1];
		final String host = args[2];
		final int nworkers = Integer.valueOf(args[3]);

		System.out.println("Setting policy");
		System.setProperty("java.security.policy", securityPolicy);

		// I download server's stubs ==> must set a SecurityManager
		System.setSecurityManager(new RMISecurityManager());

		try {
			IRMIConnector conn = (IRMIConnector) Naming.lookup("//" + host + "/" + BINDING_NAME);
			System.out.println("Connected");
			conn.getWorkQueue();
			conn.getDictionary();

			Runnable runn = new Runnable() {

				@Override
				public void run() {
					String item = "";
					while (!Thread.currentThread().isInterrupted()) {
						try {
							item = conn.getNextWord();
//							String item = queue.take();

							if (item.isEmpty() || conn.containsWord(item)) {
								continue;
							}

							conn.addWord(item);
							String url = "http://www.thesaurus.com/browse/" + URLEncoder.encode(item);

							System.out.println(item);
							// System.out.println("get " + url);
							HttpClient.Response response = HttpClient.get(url);
							// System.out.println("Response has " +
							// response.getResponse().length());
							String dt = response.getResponse();

							String pattern = "<a href=\"http://www.thesaurus.com/browse/";
							int lastIndex = dt.indexOf(pattern, 0);
							int count = 2;
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

								if (!conn.containsWord(word)) {
									conn.enqueueWord(word);
								}

								lastIndex = dt.indexOf(pattern, lastIndex + 1);

								Thread.sleep(500);
							}

							// Process item
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
							break;
						} catch (IOException e) {
							System.err.println("Error catch when trying to process word \""+item+"\"");
//							e.printStackTrace();
						}
					}

				}
			};

			System.out.println("Creating workers...");
			System.out.println(nworkers + " worker(s) will be created");
			for (int i = 0; i < nworkers; i++) {
				// workerList.add(new Worker(dict, workQueue));
				workerList.add(new Thread(runn));
			}

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.out.print("Asking clients to stop...");
					try {
						conn.interruptWorkers();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.out.print(" Failed.");
					}
					System.out.println(" Done.");
					interruptWorkers(workerList);

				}
			});

			System.out.println("Starting workers...");
			for (Thread worker : workerList) {
				worker.start();
			}

			while (!conn.shouldInterruptWorkers()) {
				Thread.sleep(1000);
			}

			interruptWorkers(workerList);

		} catch (Exception e) {
			System.out.println("RMIClient exception: " + e.getMessage());
			e.printStackTrace();
			interruptWorkers(workerList);
		}

	}

	private static void interruptWorkers(ArrayList<Thread> workerList) {
		System.out.print("Interrupting workers...");
		for (Thread worker : workerList) {
			worker.interrupt();
		}
		System.out.println(" Done.");
	}

}
