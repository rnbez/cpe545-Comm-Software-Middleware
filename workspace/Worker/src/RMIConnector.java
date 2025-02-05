import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class RMIConnector extends UnicastRemoteObject implements IRMIConnector {
	private final BlockingQueue<String> queue;
	private final ConcurrentHashMap<String, String> dict;
	private boolean shouldInterruptWorkers;

	public RMIConnector(BlockingQueue<String> workQueue, ConcurrentHashMap<String, String> dict, String bindingName)
			throws RemoteException, MalformedURLException {
		super();
		this.queue = workQueue;
		this.dict = dict;
		this.shouldInterruptWorkers = false;
		Naming.rebind(bindingName, this);
	}

	@Override
	public BlockingQueue<String> getWorkQueue() throws RemoteException {
		return this.queue;
	}

	@Override
	public ConcurrentHashMap<String, String> getDictionary() throws RemoteException {
		return this.dict;
	}

	@Override
	public void interruptWorkers() throws RemoteException {
		this.shouldInterruptWorkers = true;

	}

	@Override
	public boolean shouldInterruptWorkers() throws RemoteException {
		return this.shouldInterruptWorkers;
	}

	@Override
	public String getNextWord() throws RemoteException {
		try {
			return queue.take();
		} catch (InterruptedException e) {
			return "";
		}
	}

	@Override
	public void addWord(String word) throws RemoteException {
		this.dict.put(word, word);
	}

	@Override
	public void enqueueWord(String word) throws RemoteException {
		try {
			this.queue.put(word);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public boolean containsWord(String word) throws RemoteException {
		return this.dict.containsKey(word);
	}

}
