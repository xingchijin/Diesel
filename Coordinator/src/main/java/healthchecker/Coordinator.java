package healthchecker;

import error.WrongMessageTypeException;
import services.io.NetConfig;
import shared.ConnMetrics;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;

/**
 * Created by xingchij on 11/20/15.
 */
public class Coordinator {

	private TaskReceiver gate;
	private WatcherGroup spies;
	private MongoDBExplorer detecter;

	public Coordinator() throws SocketException {
		gate = new TaskReceiver();
		spies = new WatcherGroup();
		detecter = new MongoDBExplorer(ConnMetrics.IPsOfMongoDB);
	}

	public void run() {
		Thread reception = new Thread(gate);
		reception.start();

//		Thread dbexp = new Thread(detecter);
//		dbexp.start();
		
		while (true) {
			try {
				int identity = spies.watchForHeartBeat();

				spies.checkDead();

				if (identity == WatcherGroup.ID_PRIMARY && spies.workable == true) {
					// send task to primary when receive hearteat from primary
					gate.sendTask(new NetConfig(spies.getPrimary().getIP(), ConnMetrics.portReceiveJobs));
				}

				spies.watchForWhoIsPrimary();
			} catch (InterruptedIOException e) {
				closeConnections();
				System.out
						.println("Coordinator terminated. All resources released");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (WrongMessageTypeException e) {
				e.printStackTrace();
			}
		}
	}

	public void closeConnections() {
		if (gate != null) {
			gate.closeConnections();
		}
		if (spies != null) {
			spies.closeConnections();
		}
	}

	public static void main(String[] args) throws SocketException {
		
		Coordinator coordinator = new Coordinator();
		System.out.println("Coordinator now running...");
		coordinator.run();
		
	}
}
