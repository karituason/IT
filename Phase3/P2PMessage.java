import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;

public class P2PMessage{
	public static final byte[] PROTOCOL = new byte[]{'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','o','c','o','l'};

	public static final byte P2P_LIVE = (byte)-1;
	public static final byte P2P_CHOKE = (byte)0;
	public static final byte P2P_UNCHOKE = (byte)1;
	public static final byte P2P_INTERESTED = (byte)2;
	public static final byte P2P_UNINTERESTED = (byte)3;
	public static final byte P2P_HAVE = (byte)4;
	public static final byte P2P_REQUEST = (byte)6;
	public static final byte P2P_PIECE = (byte)7;
	public static final byte P2P_BITFIELD = (byte)5;

	public static byte[] getHandshake(byte[] hash, byte[] pID){
		byte[] msg = new byte[68];
		msg[0] = (byte)19;
		System.arraycopy(PROTOCOL, 0, msg, 1, PROTOCOL.length);
		System.arraycopy(hash, 0, msg, 28, hash.length);
		System.arraycopy(pID, 0, msg, 48, pID.length);
		return msg;
	}
	
	//For Keep alive, choke, unchoke. interested, uninterested message
	public static byte[] getMessage(byte type){
		byte[] message = new byte[5];
		byte[] prefix = getBigEndian(1);
		System.arraycopy(prefix, 0, message, 0, 4);
		if (type == P2P_CHOKE){
			message[4] = P2P_CHOKE;
		} else if (type == P2P_UNCHOKE){
			message[4] = P2P_UNCHOKE;
		} else if (type == P2P_INTERESTED){
			message[4] = P2P_INTERESTED;
		} else if (type == P2P_UNINTERESTED){
			message[4] = P2P_UNINTERESTED;
		} else {
			message = new byte[4];
		}
		
		return message;
	}
	
	//For request message
	public static byte[] getMessage(int index, int begin, int length){
		byte[] message = new byte[17];
		message[4] = P2P_REQUEST;
		byte[] prefix = getBigEndian(13);
		byte[] indexByte = getBigEndian(index);
		byte[] beginByte = getBigEndian(begin);
		byte[] lengthByte = getBigEndian(length);
		System.arraycopy(prefix, 0, message, 0, 4);
		System.arraycopy(indexByte, 0, message, 5, 4);
		System.arraycopy(beginByte, 0, message, 9, 4);
		System.arraycopy(lengthByte, 0, message, 13, 4);
		return message;
	}
	
	//For piece message
	public static byte[] getMessage(int index, int begin, byte[] block){
		byte[] message = new byte[13+block.length];
		byte[] prefix = getBigEndian(9+block.length);
		byte[] indexByte = getBigEndian(index);
		byte[] beginByte = getBigEndian(begin);
		message[4] = P2P_PIECE;
		System.arraycopy(prefix, 0, message, 0, 4);
		System.arraycopy(indexByte, 0, message, 5, 4);
		System.arraycopy(beginByte, 0, message, 9, 4);
		System.arraycopy(block, 0, message, 13, block.length);
		return message;
	}
	
	//For bit field message
	public static byte[] getMessage(int[] bitfield){
		int bitfield_size = (int) Math.ceil(1.0 * bitfield.length/8);
		byte[] message = new byte[1 + bitfield_size];
		message[0] = 5;
		byte[] field = toBitfield(bitfield);
		System.arraycopy(field, 0, message, 1, field.length);
		return message;
	}
	
	//For have message
	public static byte[] getMessage(int index){
		byte[] message = new byte[9];
		message[4] = P2P_HAVE;
		byte[] prefix = getBigEndian(5);
		System.arraycopy(prefix, 0, message, 0, 4);
		byte[] indexB = getBigEndian(index);
		System.arraycopy(indexB, 0, message, 5, 4);
		return message;
	}
	
	public static String translateMessage(byte[] message){
		String translation = "";
		if (message != null && message.length != 0){
			byte messageID = message[0];
			byte[] msgTrans;
			switch(messageID){
				case P2P_CHOKE:
					translation += "Choke";
					break;
				case P2P_UNCHOKE:
					translation += "Unchoked";
					break;
				case P2P_INTERESTED:
					translation += "Interested";
					break;
				case P2P_UNINTERESTED:
					translation += "Uninterested";
					break;
				case P2P_HAVE:
					translation += "Have: ";
					msgTrans = new byte[message.length - 1];
					System.arraycopy(message, 1, msgTrans, 0, msgTrans.length);
					int index = getInt(msgTrans);
					translation += "<Index: " + index +">";
					break;
				case P2P_REQUEST:
					translation += "Request: ";
					msgTrans = new byte[4];
					System.arraycopy(message, 1, msgTrans, 0, 4);
					translation += "<Index: " +getInt(msgTrans) +">";
					System.arraycopy(message, 5, msgTrans, 0, 4);
					translation += "<Begin: " +getInt(msgTrans) +">";
					System.arraycopy(message, 9, msgTrans, 0, 4);
					translation += "<Length: " +getInt(msgTrans)+">";
					break;
				case P2P_PIECE:
					translation += "Piece: ";
					msgTrans = new byte[4];
					System.arraycopy(message, 1, msgTrans, 0, 4);
					translation += "<Index: " + getInt(msgTrans) +">";
					System.arraycopy(message, 5, msgTrans, 0, 4);
					translation += "<Begin: " + getInt(msgTrans) +">";
					break;
				case P2P_BITFIELD:
					translation += "Bitfield: ";
					msgTrans = Arrays.copyOfRange(message, 1, message.length);
					translation += (new String(msgTrans));
					break;
				default:
					translation += "Keep-alive";
			}
		} else {
			translation = "Keep-alive";
		}
		return translation;
	}

	public static int[] translateBitfield(byte[] bitfield, int num_of_pieces){
		int[] bits = new int[num_of_pieces];
		
		for (int i = 0; i < num_of_pieces; i++){
			int index = i/8;
			int position = 8 - (i % 8) - 1;
			byte b = bitfield[index];
			bits[i] = (b >> position) & 1;
		}
		return bits;	
	}
	
	public static byte[] toBitfield(int[] bitfield){
		byte[] field = new byte[(int)Math.ceil(1.0 * bitfield.length/8)];
		int bitnum = 0;
		for (int i = 0; bitnum < bitfield.length; i++){
			byte b = 0;
			for (int j = 0; j < 8; j++){
				if (bitfield[i*8 + j] == 1){
					b |= (1 << (7-j));
				}
				bitnum++;
				if (bitnum == bitfield.length) break;
			}
			field[i] = b;
		}
		return field;
	}

	public static int getInt(byte[] bigEndian){
		ByteBuffer buf = ByteBuffer.wrap(bigEndian);
		int num = buf.getInt();
		return num;
	}
	
	public static byte[] getBigEndian(int num){
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putInt(num);
		byte[] bigEnd = buf.array();
		return bigEnd;	
	}
	

}
