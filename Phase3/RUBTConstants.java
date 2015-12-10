//Atif Siddiqi, Karissa Tuason, Daniel Chunn

public class RUBTConstants{
	
	/*number of download peers allowed*/
	public static final int download_pool = 10;

	/*number of upload peers allowed*/
	public static final int upload_pool = 10;
	
	public static final String[] accepted_upload_peers = {"128.6.171.132", "128.6.171.131", "128.6.171.130"};
	
	public static final String[] accepted_download_peers = {"128.6.171.132", "128.6.171.131", "128.6.171.130"};
	
	/*number of seconds socket method timeout is*/
	public static final int timeout_interval = 120000;

	/*Hex array for converting to hex string*/
	public static final char[] HEX = "0123456789ABCDEF".toCharArray();

	/*What peer id will start with*/
	public static final byte[] PID_HEAD = {'K', 'A', 'T'};

	public static String toHexString(byte[] bytes) {
		if (bytes == null || bytes.length == 0){
			return null;
		}
		char[] hexArr = new char[bytes.length * 3];

		for (int i = 0; i < bytes.length; i++) {
			int temp = bytes[i] & 0xFF;
			hexArr[i * 3] = '%';
			hexArr[i * 3 + 1] = RUBTConstants.HEX[(temp >> 4) & 0x0F];
			hexArr[i * 3 + 2] = RUBTConstants.HEX[temp & 0x0F];
		}

		return new String(hexArr);
	}
	
}
