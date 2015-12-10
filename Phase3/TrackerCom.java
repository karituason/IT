//Atif Siddiqi, Karissa Tuason, Daniel Chunn

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.lang.Thread;

import GivenTools.BencodingException;
import GivenTools.TorrentInfo;

public class TrackerCom implements Runnable{
	public static final String TRKEY_HASH = "info_hash";
	public static final String TRKEY_PID = "peer_id";
	public static final String TRKEY_PORT = "port";
	public static final String TRKEY_UP = "uploaded";
	public static final String TRKEY_DOWN = "downloaded";
	public static final String TRKEY_LEFT = "left";
	public static final String TRKEY_EVENT = "event";
	public static final String TR_START = "started";
	public static final String TR_COMPLETE = "completed";
	public static final String TR_STOP = "stopped";
	public static final String TR_EMPTY = "empty";
	
	class PeerList{
		ArrayList<PeerInfo> peerList;
		public PeerList(){
			peerList = null;
		}
	}
	
	private boolean debug = true;
	private int currentPort;
	private int interval;
	private int min_interval;
	private TorrentInfo torrent_info;
	private byte[] peer_id;
	private String event;
	private volatile LockedVariables var;
	private volatile PeerList peerList;
	private boolean willDownload = true;
	private boolean running = true;
	private String name = "Tracker Com";
	private DownloadPeerMaker dpm;
	private Thread downloader;
	
	public TrackerCom(TorrentInfo torrent_info, byte[] peer_id, LockedVariables var, int port){
		this.torrent_info = torrent_info;
		this.peer_id = peer_id;
		this.var = var;
		this.currentPort = port;
		this.interval = 0;
		peerList = new PeerList();
		
	}
	public void run(){
		if (debug) System.out.println(name +": starting");
		boolean go = false;
		while (!go){
			synchronized(var){
				if (var.filed){
					go = true;
				}
			}
		}
		synchronized (var){
			if (var.left == 0){
				willDownload = false;
			}
		}
		event = TR_START;
		URL url = getFullURL(event);
		byte[] response = hTTPResponseFromGet(url);
		try {
			ResponseInfo parsedResponse = new ResponseInfo(response);
			interval = parsedResponse.interval;
			min_interval = parsedResponse.min_interval;
			synchronized (peerList){
				peerList.peerList = parsedResponse.getPeers();
				dpm = new DownloadPeerMaker(torrent_info, peerList, var, peer_id);
				downloader = new Thread(dpm);
				downloader.start();
			}
		} catch (BencodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (running){
			try {
				Thread.sleep((int)(min_interval * 1000 + 0.5*(interval  - min_interval) * 1000));
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				running = false;
				continue;
			}
			if (Thread.interrupted()){
				running = false;
				continue;
			}
			event = TR_EMPTY;
			if (willDownload){
				synchronized (var){
					if (var.left == 0){
						willDownload = false;
						event = TR_COMPLETE;
					}
				}
			}
			url = getFullURL(event);
			response = hTTPResponseFromGet(url);
			try {
				ResponseInfo parsedResponse = new ResponseInfo(response);
				interval = parsedResponse.interval;
				min_interval = parsedResponse.min_interval;
				synchronized (peerList){
					peerList.peerList = parsedResponse.getPeers();
				}
			} catch (BencodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//Send stop
		event = TR_STOP;
		url = getFullURL(event);
		response = hTTPResponseFromGet(url);
		//interrupt all threads created by this thread
		if (downloader.isAlive()){
			downloader.interrupt();
		}
		//join all threads created by this thread
		try {
			downloader.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Closing Tracker Com");
	}
	
	public byte[] hTTPResponseFromGet(URL url){
		if (url == null){
			return null;
		}
		byte[] response = null;
		HttpURLConnection con = null;

		try{
			con = (HttpURLConnection) url.openConnection();
		} catch (IOException e){
			return null;
		}

		try{
			con.setRequestMethod("GET");
		} catch (ProtocolException e){
			return null;
		} catch (SecurityException e){
			return null;
		}
		int length = con.getContentLength();
		try {
			int responseCode = con.getResponseCode();
		
			/*System.out.println("GET Response Code: "+ responseCode);
			/**/

			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream is = con.getInputStream();
				response = new byte[length];
				is.read(response);
				is.close();
	        } else {
				System.out.println("Get fail");
				return null;
			}
		} catch (IOException e){
			return null;
		}

		return response;
	}

	public URL getFullURL(String event){ //variables have changed update
		if (debug) System.out.println(event);
		URL url = null;

		if (peer_id == null || peer_id.length != 20){
			return null;
		}

		String info_hash = RUBTConstants.toHexString(torrent_info.info_hash.array());
		String IDStr = RUBTConstants.toHexString(peer_id);
		String urlString = torrent_info.announce_url.toString() + "?";
		synchronized(var){
		//concatenate get parameters to url
		
			urlString += TrackerCom.TRKEY_HASH + "=" + info_hash + "&";
			urlString += TrackerCom.TRKEY_PID + "=" + IDStr + "&";
			urlString += TrackerCom.TRKEY_PORT + "=" + currentPort + "&";
			urlString += TrackerCom.TRKEY_UP + "=" + var.uploaded + "&";
			urlString += TrackerCom.TRKEY_DOWN + "=" + var.downloaded + "&";
			urlString += TrackerCom.TRKEY_LEFT + "=" + var.left + "&";
			urlString += TrackerCom.TRKEY_EVENT + "=";
		}
		if (event.equals(TR_START)){
			urlString += TrackerCom.TR_START;
		} else if (event.equals(TR_COMPLETE)){
			urlString += TrackerCom.TR_COMPLETE;
		} else if (event.equals(TR_STOP)){
			urlString += TrackerCom.TR_STOP;
		} else {
			urlString += TrackerCom.TR_EMPTY;
		}

		if (debug) System.out.println(urlString);

		try {
			url = new URL(urlString);
		} catch (MalformedURLException e){
			return null;
		} 
		return url;
	}

}

