//Atif Siddiqi, Karissa Tuason, Daniel Chunn

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

import GivenTools.TorrentInfo;


public class PeerDownload implements Runnable{
	LockedVariables var;
	boolean debug = false;
	boolean debug2 = true;
	private byte[] peer_id;
	private Socket sock = null;
	private DataOutputStream dout = null;
	private DataInputStream din = null;
	private OutputStream out = null;
	private InputStream in = null;
	private final TorrentInfo torrent_info;
	private int[] bitfield = null;
	private boolean choked = true;
	private PeerInfo peer;
	private String name;
	private DownloadPeerMaker.PeerStack stack;
	
	public PeerDownload(byte[] peer_id, TorrentInfo torrent_info, PeerInfo peer, LockedVariables var, DownloadPeerMaker.PeerStack stack){
		if (peer == null || torrent_info == null || peer_id == null || var == null){
			//throw exception;
		}
		name = "Download Peer[" + peer.ip + "]";
		this.peer = peer;
		this.torrent_info = torrent_info;
		this.peer_id = peer_id;
		this.var = var;
		this.stack = stack; 
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		boolean go = false;
		boolean start = false;
		try{
			if (this.openSocket() == 1 && this.shakeHands() == 1){
				System.out.println("Starting "+ name);
				start = true;
				while (!go){
					synchronized(var){
						if (var.filed){
							go = true;
						}
					}
				}
				if (!Thread.interrupted()){
					downloadFile();
					synchronized (var){
						for (int i = 0; i< bitfield.length; i++){
							if (bitfield[i] == 1){
								var.removeIndex(i);
							}
						}
					}
					sock.close();
					din.close();
					dout.close();
				}
			}
		} catch (Exception e) {
			
		}
		synchronized(stack){
			stack.closed_peers.push(peer);
		}
		if (start) System.out.println("Closing " + name);
	}
	
	private int openSocket(){
		if (debug) System.out.println(name + ": In open socket");
		if (peer.ip.equals("128.6.171.132")) return 0;
		try{
			sock = new Socket(peer.ip, peer.port);
			in = sock.getInputStream();
			out = sock.getOutputStream();
			din = new DataInputStream(in);
			dout = new DataOutputStream(out);
			return 1;
		} catch (IOException e){
			//handle exception
		} catch (Exception e){
			//handle exception

		}
		return 0;
	}
	
	private int shakeHands(){
		if (debug) System.out.println(name + ": In handshake");
		byte[] handshake = P2PMessage.getHandshake(torrent_info.info_hash.array(), peer_id);
		try{
			dout.write(handshake);
			dout.flush();
			sock.setSoTimeout(RUBTConstants.timeout_interval);

			byte[] their_shake = new byte[68];
			din.readFully(their_shake);
			if (debug) System.out.println(new String(peer.PID));
			byte[] hash = Arrays.copyOfRange(their_shake, 28, 48);
			byte[] their_id = Arrays.copyOfRange(their_shake, 48, 68);
			if (debug) System.out.println(new String(their_id));
			if (debug) System.out.println(new String(hash));
			if (debug) System.out.println(new String(torrent_info.info_hash.array()));
			if (Arrays.equals(torrent_info.info_hash.array(), hash) && Arrays.equals(peer.PID, their_id)){
				if(debug) System.out.println(name + ": leaving handshake");
				return 1;
			} else {
				if(debug) System.out.println(name + ": bad handshake");
				return 0;
			}
		} catch (Exception e){
			//handle exception
		}

		return 0;
	}
	
	private byte[] getMessage() throws InterruptedException{
		byte[] prefix = new byte[4];
		byte[] response_message = null;
		try{
			sock.setSoTimeout(2000);
			for (int i = 0; i < 4; i++){
				prefix[i] = din.readByte();
			}
			int messageLength = P2PMessage.getInt(prefix);
			response_message = new byte[messageLength];
			for (int i = 0; i < messageLength; i++){
				response_message[i] = din.readByte();
			}
			String translation = P2PMessage.translateMessage(response_message);
			if (debug) System.out.println(name + ": " + translation);
		} catch (Exception e){
			throw new InterruptedException("in get Message");
		}
		return response_message;
	}
	
	private boolean downloadFile() throws InterruptedException{
		int number_of_pieces = (int)Math.ceil(1.0 * torrent_info.file_length/torrent_info.piece_length);
		int last_piece_size = torrent_info.file_length % torrent_info.piece_length == 0? 
								torrent_info.piece_length:
								torrent_info.file_length % torrent_info.piece_length;
		byte[] currentPiece;
		byte[] response_message = null;
		
		
		bitfield = new int[number_of_pieces];
		boolean haveMessage = true;
		//get Bitfield or have messages
		while (haveMessage){
			try{
				response_message = getMessage();
				if (Thread.interrupted()){
					throw new InterruptedException("Error at Getting first message after shake");
				}
				if (response_message.length > 0){
					byte messageID = response_message[0];
					if (messageID == (byte)5){
						bitfield = P2PMessage.translateBitfield(Arrays.copyOfRange(response_message, 1, response_message.length), number_of_pieces);
						haveMessage = false;
					} else if (messageID == (byte)4) {
						int index = P2PMessage.getInt(Arrays.copyOfRange(response_message, 1, response_message.length));
						bitfield[index] = 1;
						haveMessage = false;
					} else if (messageID == (byte)0){
						//keep alive
					} else {
						throw new InterruptedException("Error at Getting first message after shake");
					}
				}
			} catch (Exception e){
				//
			}
		}
		boolean interest = false;
		for (int i = 0; i< bitfield.length; i++){
			if (bitfield[i] == 1){
				interest = true;
				synchronized(var){
					var.processHave(i);
				}
			}
		}
		
		if (!interest) return false; //close	
		//bitfield or have?
		byte[] interestedMessage = P2PMessage.getMessage((byte)2);
		try {
			out.write(interestedMessage);
			out.flush();
			sock.setSoTimeout(RUBTConstants.timeout_interval);
		} catch(Exception e){
			e.printStackTrace();
		}
		waitForUnchoke();
		boolean running = true;
		if (Thread.interrupted()){
			running = false;
		}
		//download
		boolean to_download = false;
		while (running){
			// choose a piece
			if (Thread.interrupted()){
				running = false;
				continue;
			}
			int index;
			synchronized (var){
				int l = var.left;
				if (l == 0){
					running = false;
					continue;
				}
				index = var.seeNext();
				if (bitfield[index] == 1){
					var.take();
					var.piece_status[index] = 2;
					to_download = true;
				}
			}
			if (to_download){
				to_download = false;
				boolean match = false;
				while(!match){
					if (index == (bitfield.length -1)){
						currentPiece = downloadPiece(index, last_piece_size);
					} else {
						currentPiece = downloadPiece(index, torrent_info.piece_length);
					}
				
					match = checkSha1(currentPiece, index);
					if (match){					
					//add piece to arraylist
					//	System.out.println(name + ": waiting on var");
						synchronized (var){
						//	System.out.println(name + ": have var");
							var.pieces[index] = currentPiece;
							var.piece_status[index] = 1;
							
/**********/				if (debug2) System.out.println(name + ": Adding [" + index+"] to stack");
							
							var.addFileIndex(index);
						} 
					} else {
						if (debug) System.out.println(name + ": Did not match");
					}
				}
			}
		}

		return true;
		
	}
	
	private int waitForUnchoke(){
		byte[] prefix = new byte[4];
		byte[] response_message = null;
		if (debug) System.out.println(name + ": Waiting for unchoke");
		while (choked){
			try{
				for (int i = 0; i < 4; i++){
					prefix[i] = din.readByte();
				}
				int messageLength = P2PMessage.getInt(prefix);
				response_message = new byte[messageLength];
				for (int i = 0; i < messageLength; i++){
					response_message[i] = din.readByte();
				}
				String translation = P2PMessage.translateMessage(response_message);
				if (debug) System.out.println(name + ": "+ translation);
				if (messageLength >0){
					byte messageID = response_message[0];
					if (messageID == (byte)1){
						choked = false;
					} else if (messageID == (byte)4) {
						int index = P2PMessage.getInt(Arrays.copyOfRange(response_message, 1, response_message.length));
						bitfield[index] = 1;
						synchronized(var){
							var.processHave(index);
						}
					}
				}
			} catch (Exception e){
				Thread.currentThread().interrupt();
				return 0;
			} 
		}
		return 1;
	}
	
	private byte[] downloadPiece(int index, int size) throws InterruptedException{
		byte[] piece = new byte[size];
		int left_of_piece = size;
		int getSize = 16384;
		boolean whole_piece = false;
		byte[] requestMessage;
		byte[] response;
		byte[] prefix = new byte[4];
		int begin = 0;
		while (!whole_piece){
			if (Thread.interrupted()){
				throw new InterruptedException();
			}
			if (left_of_piece > getSize){
				requestMessage = P2PMessage.getMessage(index, begin, getSize);
				try{
					out.write(requestMessage);
					out.flush();
					sock.setSoTimeout(RUBTConstants.timeout_interval);
					for (int j = 0; j < 4; j++){
						prefix[j] = din.readByte();
					}
					int messageLength = P2PMessage.getInt(prefix);
					response = new byte[messageLength];
					for(int j = 0; j < messageLength; j++){
						response[j] = din.readByte();
					}
					String translation = P2PMessage.translateMessage(response);
					if (debug) System.out.println(name + ": " + translation);
					String expectedPrefix = "Piece: <Index: " + index + "><Begin: " + begin + ">";
					if (translation.equals(expectedPrefix)){
						System.arraycopy(response, 9, piece, begin, response.length -9);
						begin += getSize;
						left_of_piece -= getSize;
					} else if (translation.startsWith("Choke")){
						choked = true;
					}
				} catch (Exception e){
					//handle exception;
				}
			} else {
				getSize = left_of_piece;
				whole_piece = true;
				requestMessage = P2PMessage.getMessage(index, begin, getSize);

				
	
				try{
					out.write(requestMessage);
					out.flush();
					sock.setSoTimeout(RUBTConstants.timeout_interval);
					for (int j = 0; j < 4; j++){
						prefix[j] = din.readByte();
					}
					int messageLength = P2PMessage.getInt(prefix);
					response = new byte[messageLength];
					for(int j = 0; j < messageLength; j++){
						response[j] = din.readByte();
					}
					String translation = P2PMessage.translateMessage(response);
					if (debug) System.out.println(name + ":" + translation);
					String expectedPrefix = "Piece: <Index: " + index + "><Begin: " + begin + ">";
					if (translation.equals(expectedPrefix)){
						System.arraycopy(response, 9, piece, begin, response.length -9);
						begin += getSize;
						left_of_piece -= getSize;
					} else if (translation.startsWith("Choke")){
						choked = true;
					}
				} catch (Exception e){
					//handle exception;
				}
			}
		}
		return piece;
	}	
	
	private boolean checkSha1(byte[] piece, int index){
		boolean match = false;
		byte[] test = null;
		byte[] pieceCopy = Arrays.copyOf(piece, piece.length);
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			test = md.digest(pieceCopy);
		} catch (Exception e) {
			if (debug) System.out.println(name + ": Something happened in sha");
			return false;
		}
		ByteBuffer buf = torrent_info.piece_hashes[index];
		byte[] hash = buf.array();
		if (Arrays.equals(test, hash)){
			match = true;
		}
		return match;
	}
}
