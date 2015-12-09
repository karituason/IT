import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;

public class PeerInfo{
	
	public final static ByteBuffer KEY_PID = ByteBuffer.wrap(new byte[]{'p', 'e', 'e', 'r', ' ', 'i', 'd'});
	
	public final static ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[]{'i', 'p'});
	
	public final static ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[]{'p', 'o', 'r', 't'});

	public final Map<ByteBuffer, Object> peer;
	
	public final byte[] PID;
	public final String ip;
	public final int port;

	public PeerInfo(Map<ByteBuffer, Object> peer) throws IOException{
		if (peer == null || peer.size() == 0){
			throw new IllegalArgumentException("Peer maps can't be null or empty");
		}

		this.peer = peer;

		ByteBuffer PIDBuff = (ByteBuffer)this.peer.get(PeerInfo.KEY_PID);
		if (PIDBuff == null){
			throw new IOException("Array object bad format.");
		} 
		
		this.PID = PIDBuff.array();

		ByteBuffer ipBuff = (ByteBuffer)this.peer.get(PeerInfo.KEY_IP);
		
		if(ipBuff == null){
			throw new IOException("Array object bad format.");
		}
		
		try {
			this.ip = new String(ipBuff.array(), "ASCII");
		} catch (UnsupportedEncodingException e){
			throw new IOException("Couldn't extract peer ip");
		}

		Integer port = (Integer)this.peer.get(PeerInfo.KEY_PORT);
		if (port == null){
			throw new IOException("Array object bad format.");
		}
		this.port = port;

	}
	
	public String toString(){
		String ret = ip + ": id = " + PID.toString() + ", port = " + port;
		return ret;
	}
}
