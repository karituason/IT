import java.util.ArrayList;
import java.util.Stack;

import GivenTools.TorrentInfo;


public class DownloadPeerMaker implements Runnable{
	byte[] peer_id;
	TorrentInfo torrent_info;
	volatile TrackerCom.PeerList peers;
	ArrayList<PeerInfo> usedPeers;
	ArrayList<PeerInfo> livePeers;
	volatile LockedVariables var;
	class PeerStack{
		Stack<PeerInfo> closed_peers;
	}
	
	public DownloadPeerMaker(TorrentInfo torrent_info, TrackerCom.PeerList peers, LockedVariables var, byte[] peer_id){
		this.peer_id = peer_id;
		this.torrent_info = torrent_info;
		this.peers = peers;
		this.var = var;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
