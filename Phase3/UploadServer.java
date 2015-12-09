import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import GivenTools.TorrentInfo;


public class UploadServer implements Runnable {
	public ServerSocket s = null;
	int port = 6881;
	int maxport = 6889;
	String name = "UploadServer";
	boolean opened = false;
	boolean running = true;
	byte[] peer_id;
	TorrentInfo torrent_info;
	volatile LockedVariables var;
	Thread tracker;
	ArrayList<Thread> peers;
	boolean debug = true;
	
	public UploadServer(TorrentInfo torrent_info, byte[] peer_id, LockedVariables var){
		this.torrent_info = torrent_info;
		this.peer_id = peer_id;
		this.var = var;
		peers = new ArrayList<Thread>();
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
				break;
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
			try {
				//check for upload pool capacity
				//create a new upload thread
				
				if (peers.size() < RUBTConstants.upload_pool){
					if (debug) System.out.println(name + " waiting for accept");
					Socket socket = s.accept();
					PeerUpload peerup = new PeerUpload(socket, peer_id, torrent_info, var);
					if (debug) System.out.println("Got accept");
					Thread pUp = new Thread(peerup);
					pUp.start();
					peers.add(pUp);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
			} catch (Exception e){
				//
			}
		}
		
		if (!s.isClosed()){
			try {
				s.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
			}
		}
		
		//interrupt all threads
		if (tracker.isAlive()){
			tracker.interrupt();
		}
		//join all threads
		try {
			tracker.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
		}
		for (int i = 0; i < peers.size(); i++){
			peers.get(i).interrupt();
		}
		
		System.out.println("Closing Upload Server");
	}
}
