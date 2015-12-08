import java.io.IOException;
import java.net.ServerSocket;

import GivenTools.TorrentInfo;


public class UploadServer implements Runnable {
	ServerSocket s = null;
	int port = 6881;
	int maxport = 6889;
	String name = "UploadServer";
	boolean opened = false;
	boolean running = true;
	byte[] peer_id;
	TorrentInfo torrent_info;
	volatile LockedVariables var;
	Thread tracker;
	
	public UploadServer(TorrentInfo torrent_info, byte[] peer_id, LockedVariables var){
		this.torrent_info = torrent_info;
		this.peer_id = peer_id;
		this.var = var;
	}
	//keep track of the number of download peers opened and visited
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		//open a ServerSocket
		for (; port <= maxport && !opened; port++){
			try {
				s = new ServerSocket(port);
				opened = true;
				//start tracker
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
		
		if (!s.isClosed()){
			tracker = new Thread(new TrackerCom(torrent_info, peer_id, var, port));
			tracker.start();
		}
		
		while (running){
			if (Thread.interrupted()){
				running = false;
				continue;
			}
			/*try {
				//check for upload pool capacity
				//s.accept();
				//create a new upload thread
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
		
		if (!s.isClosed()){
			try {
				s.close();
				//interrupt all threads
				if (tracker.isAlive()){
					tracker.interrupt();
				}
				//join all threads
				tracker.join();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Closing Upload Server");
	}
}
