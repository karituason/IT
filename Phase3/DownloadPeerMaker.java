import java.util.ArrayList;
import java.util.Stack;

import GivenTools.TorrentInfo;


public class DownloadPeerMaker implements Runnable{
	byte[] peer_id;
	TorrentInfo torrent_info;
	volatile TrackerCom.PeerList peers;
	ArrayList<String> usedPeers;
	ArrayList<String> livePeers;
	ArrayList<Thread> peerThreads;
	volatile LockedVariables var;
	class PeerStack{
		Stack<PeerInfo> closed_peers;
		public PeerStack(){
			closed_peers = new Stack<PeerInfo>();
		}
	}
	volatile PeerStack closed;
	
	public DownloadPeerMaker(TorrentInfo torrent_info, TrackerCom.PeerList peers, LockedVariables var, byte[] peer_id){
		this.peer_id = peer_id;
		this.torrent_info = torrent_info;
		this.peers = peers;
		this.var = var;
		usedPeers = new ArrayList<String>();
		livePeers = new ArrayList<String>();
		peerThreads = new ArrayList<Thread>();
		closed = new PeerStack();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Starting download maker");
		boolean running = true;
		int i = 0;
		while (running){
			//create peerDownload threads
			if (Thread.interrupted()){
				System.out.println ("Download Maker Interrupted");
				running = false;
				continue;
			}
			synchronized (closed){
				while (!closed.closed_peers.isEmpty()){
					PeerInfo cp = closed.closed_peers.pop();
					Thread tmp = peerThreads.get(livePeers.indexOf(cp.toString()));
					peerThreads.remove(tmp);
					livePeers.remove(cp.toString());
					//System.out.println(livePeers.size());
					usedPeers.add(cp.toString());
					if (tmp.isAlive()){
						tmp.interrupt();
					}
				}
			}
			boolean go = false;
			while (!go){
				synchronized(var){
					if (var.filed){
						go = true;
					}
				}
			}
			i++;
			synchronized(var){
				if (var.left == 0){
					running = false;
					continue;
				}
			}
			if (livePeers.size() < RUBTConstants.download_pool){
				PeerInfo peer = null;
				synchronized(peers){
					for (int i1 = 0; i1 < peers.peerList.size(); i1++){
						peer = peers.peerList.get(i1);
						if (livePeers.contains(peer.toString()) || (usedPeers.contains(peer.toString()))){
							peer = null;
						} else {
							livePeers.add(peer.toString());
							break;
						}
					}
				}
				if (peer != null){
					PeerDownload p = new PeerDownload(peer_id, torrent_info, peer, var, closed);
					Thread t = new Thread(p);
					t.start();
					peerThreads.add(t);
				}
			}
		}
		for (int k = 0; k < peerThreads.size(); k++){
			if (peerThreads.get(k).isAlive()){
				peerThreads.get(k).interrupt();
			}
		}
		System.out.println("Download maker Closing");
	}
}

