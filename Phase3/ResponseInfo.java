import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.Object;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import GivenTools.ToolKit;
import GivenTools.TorrentInfo;

public class ResponseInfo{

	/*key used to retrieve failure message*/
	public final static ByteBuffer KEY_FAIL = ByteBuffer.wrap(new byte[]{'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o', 'n'});

	/*key used to retrieve interval*/
	public final static ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[]{'i', 'n', 't', 'e', 'r', 'v', 'a', 'l'});

	/*key used to retriece mininterval*/
	public final static ByteBuffer KEY_MIN_INTERVAL = ByteBuffer.wrap(new byte[]{'m', 'i', 'n', '_', 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l'});
	
	/*key used to retrieve list of peers*/
	public final static ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[]{'p', 'e', 'e', 'r', 's'});

	/*Byte array containing bytes of tracker response*/
	public final byte[] responseBytes;
	
	/*Map of decoded bytes*/
	public final Map<ByteBuffer, Object> responseMap;

	/*failure message*/
	public final String failMessage;

	/*number of seconds to wait between regular rerequests*/
	public final int interval;

	/*minimum number of seconds to wait between requests*/
	public final int min_interval;
	 
	/*ArrayList of Peers (dictionaries corresponding to peers)*/
	public final ArrayList<PeerInfo> peers;	

	@SuppressWarnings("unchecked")
	public ResponseInfo(byte[] responseBytes) throws BencodingException{
		if (responseBytes == null || responseBytes.length == 0){
			throw new IllegalArgumentException("Response bytes can't be null or empty");
		}

		this.responseBytes = responseBytes;
		
		//decode bytes into responseMap
		this.responseMap = (Map<ByteBuffer,Object>)Bencoder2.decode(responseBytes);
	
		if (responseMap == null){
			throw new BencodingException("Could not decode");
		}

		//Extract fail message
		if (responseMap.containsKey(ResponseInfo.KEY_FAIL)){
			ByteBuffer failmBuff = (ByteBuffer)this.responseMap.get(ResponseInfo.KEY_FAIL);
			
			try{
				this.failMessage = new String(failmBuff.array(), "ASCII");
				throw new BencodingException(this.failMessage);
			} catch (UnsupportedEncodingException e){
				throw new BencodingException(e.getLocalizedMessage());
			}
		} else {
			this.failMessage = "NOT_FAIL";
		}

		//Extract interval
		if (this.failMessage.equals("NOT_FAIL")){
			Integer interval = (Integer)this.responseMap.get(ResponseInfo.KEY_INTERVAL);

			if (interval == null){
				this.interval = 180;	
			} else {
				this.interval = interval.intValue();
			}

			Integer min_interval = (Integer)this.responseMap.get(ResponseInfo.KEY_MIN_INTERVAL);

			if (min_interval == null){
				this.min_interval = this.interval/2;
			} else {
				this.min_interval = min_interval.intValue();			
			}

			ArrayList<Map<ByteBuffer, Object>> peerArrBuff = (ArrayList<Map<ByteBuffer, Object>>)responseMap.get(ResponseInfo.KEY_PEERS);

			if (peerArrBuff == null || peerArrBuff.size() == 0){
				throw new BencodingException ("Couldn't get peer array");
			} else {
				Map<ByteBuffer, Object> peerBuff;
				this.peers = new ArrayList<PeerInfo>();

				for (int i = 0; i < peerArrBuff.size(); i++){
					try{
						peerBuff = (Map<ByteBuffer, Object>) peerArrBuff.get(i);
					} catch (IndexOutOfBoundsException e) {
						throw new BencodingException("Index Out of Bounds.");
					}
					

					PeerInfo peer = null;
					try {
						peer = new PeerInfo(peerBuff);
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException(e.getLocalizedMessage());
					} catch (IOException e){
						throw new BencodingException(e.getLocalizedMessage());
					}
					 
					this.peers.add(peer);						
				}
			}


			//System.out.println("peerBuff = " + peerBuff.toString());
		} else {
			//Response fail so no interval or peers
			this.interval = -1;
			this.peers = null;
			this.min_interval = -1;
		}
	}

	public ArrayList<PeerInfo> getPeers(){
		ArrayList<PeerInfo> accepted_peers = new ArrayList<PeerInfo>();

		for (int i = 0; i< this.peers.size(); i++){
			PeerInfo curr = this.peers.get(i);
			for(int j = 0; j < RUBTConstants.accepted_download_peers.length; j++){
				if (curr.ip.equals(RUBTConstants.accepted_download_peers[j])){								
					accepted_peers.add(curr);
				}
			}
		}
		return accepted_peers;
	}
}
