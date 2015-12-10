//Atif Siddiqi, Karissa Tuason, Daniel Chunn

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Stack;

import GivenTools.TorrentInfo;


public class PeerUpload implements Runnable{
	private Socket sock = null;
	private DataOutputStream dout = null;
	private DataInputStream din = null;
	private OutputStream out = null;
	private InputStream in = null;
	private final TorrentInfo torrent_info;
	volatile LockedVariables var;
	private byte[] peer_id;
	private String name;
	boolean debug = false;
	private int[] bitfield;
	Stack<Integer> new_pieces;
	private boolean debug2 = true;
	
	public PeerUpload(Socket sock, byte[] peer_id, TorrentInfo torrent_info, LockedVariables var){
		this.sock = sock;
		this.peer_id = peer_id;
		this.torrent_info = torrent_info;
		this.var = var;
		this.name = "PeerUpload[" + sock.getInetAddress().getHostAddress()+"]";
		bitfield = new int[(int)Math.ceil(1.0 * torrent_info.file_length/torrent_info.piece_length)];
		new_pieces = new Stack<Integer>();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Starting "+ name);
		try{
			in = sock.getInputStream();
			out = sock.getOutputStream();
			din = new DataInputStream(in);
			dout = new DataOutputStream(out);
		} catch (Exception e){
			//
		}
		if (shakeHands() == 1){
			getBitField();
			//have messages;
			try {
				sendHavePieces();
				waitForInterested();
				if (debug) System.out.println("got interested");
				sendUnchoke();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				if (debug2) System.out.println("Exception While sending have messages");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				if (debug2) System.out.println("Exception sent wrong message");
			}
			boolean running = true;
			while (running){
				if (Thread.interrupted()){
					running = false;
					continue;
				}
				try {
					getRequest();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					running = false;
				}
			}
		}
		try {
			sock.close();
			din.close();
			dout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//
		}
		
		System.out.println("closing "+ name);
	}
	
	private void getRequest() throws Exception{
		byte[] message = getMessage();
		String translation = P2PMessage.translateMessage(message);
		if (debug) System.out.println(translation);
		if (message.length == 0) return;
		byte msgID = message[0];
		if (msgID == (byte)6){
			if (debug) System.out.println("here");
			int index = P2PMessage.getInt(Arrays.copyOfRange(message, 1, 5));
			int begin = P2PMessage.getInt(Arrays.copyOfRange(message, 5, 9));
			int length = P2PMessage.getInt(Arrays.copyOfRange(message, 9, 13));
			if (debug) System.out.println(index+ " "+  begin +" "+ length);
			if (debug) System.out.println(bitfield[index]);
			if (bitfield[index] == 1){
				if (debug) System.out.println (bitfield[index]);
				synchronized (var){
					if (debug) System.out.println("have var");
					byte[] tmp = var.pieces[index];
					if (tmp == null) if (debug) System.out.println("null");
					if (debug) System.out.println(tmp.length);
					if (tmp != null && tmp.length >= (length+begin)){
						byte[] block = Arrays.copyOfRange(tmp, begin, begin + length);
						var.uploaded += length;
						message = P2PMessage.getMessage(index, begin, block);
						if (debug) System.out.println(P2PMessage.translateMessage(Arrays.copyOfRange(message, 4, length + 9)));
						if (debug2 ) System.out.println(name +": Sending block " + index);
						sendMessage(message);
					}
				}
			}
		} else if (msgID == P2PMessage.P2P_HAVE){
			byte[] msg = P2PMessage.getMessage(P2PMessage.P2P_UNINTERESTED);
			sendMessage(msg);
		} else {
		
			throw new IOException("wrong message");
		}
	}
	
	private void sendUnchoke() throws IOException{
		byte[] message = P2PMessage.getMessage((byte)1);
		sendMessage(message);
	}
	
	private void waitForInterested() throws InterruptedException, IOException{
		if (debug) System.out.println("Waiting for Interested");
		boolean interested = false;
		boolean notinterested = false;
		while (!interested){
			byte[] response_message = getMessage();
			if (response_message.length == 0) continue;
			byte msgID = response_message[0];
			if (msgID == (byte)2){
				if (debug) System.out.println("here");
				interested = true;
				notinterested = false;
				//send unchoke
			} else if (msgID == (byte)3){
				notinterested = true;
				interested = true;
			} else if (msgID == P2PMessage.P2P_BITFIELD || msgID == P2PMessage.P2P_HAVE){
				byte[] msg = P2PMessage.getMessage(P2PMessage.P2P_UNINTERESTED);
				sendMessage(msg);
			} else {
				if (debug) System.out.println("here2");
				interested = true;
				notinterested = true;
			}
		}
		if (notinterested){
			throw new InterruptedException("wrong message");
		} else {
			if (debug) System.out.println("interested");
		}
	}
	
	private byte[] getMessage() throws InterruptedException{
		if (debug) System.out.println("in getMessage");
		byte[] prefix = new byte[4];
		byte[] response_message = null;
		try{
			sock.setSoTimeout(30000);
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
		if (debug) System.out.println("leaving get Message");
		if (debug) System.out.println(P2PMessage.translateMessage(response_message));
		return response_message;
	}
	
	private void getBitField(){
		synchronized(var){
			for (int i = 0; i < bitfield.length; i++){
				if (var.piece_status[i] == 1){
					bitfield[i] = 1;
					new_pieces.push(i);
				}
			}
		}
	}
	
	private void updateBitField(){
		synchronized(var){
			for(int i = 0; i< bitfield.length; i++){
				if (var.piece_status[i] == 1 && bitfield[i] == 0){
					bitfield[i] =1;
					new_pieces.push(i);
				}
			}
		}
	}
	
	private void sendHavePieces() throws IOException{
		while (!new_pieces.isEmpty()){
			byte[] message = P2PMessage.getMessage(new_pieces.pop());
			sendMessage(message);
		}
	}
	
	private void sendMessage(byte[] message) throws IOException{
		try{
			dout.write(message);
			dout.flush();
		} catch (Exception e){
			throw new IOException("cant send message");
		}
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
			
			byte[] hash = Arrays.copyOfRange(their_shake, 28, 48);
			byte[] their_id = Arrays.copyOfRange(their_shake, 48, 68);
			if (debug) System.out.println(new String(their_id));
			if (debug) System.out.println(new String(hash));
			if (Arrays.equals(torrent_info.info_hash.array(), hash)){
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
	
	
}
