import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.lang.Object;
import java.lang.Thread;

import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import GivenTools.TorrentInfo;

public class RUBTClient {
	
	/*What peer id will start with*/
	private static final byte[] PID_HEAD = {'K', 'A', 'T'};
	private static void printUsage(){
		System.out.println("Usage: RUBTClient <torrent file> <save file>");
	}
	
	private void printHelp() {
		System.out.println("Possible commands:");
		System.out.println("help : This message.");
		System.out.println("quit : Quit.");
		System.out.println("pause : Pause.");
		System.out.println("resume : Resume.");
	}
	
	private static TorrentInfo getInfo(String filename){
		TorrentInfo torrentInfo = null;
		File torrFile = null;
		FileInputStream fin = null;
		byte[] torrentFile;
		byte[] buff = new byte[1024];
		
		//Convert torrentfile into byte array
		try{
			torrFile = new File(filename);
		} catch(NullPointerException e){
			System.out.println("Make sure to input a file");
			return null;
		}
		
		try{
			fin = new FileInputStream(torrFile);
		} catch (FileNotFoundException e){
			System.out.println("File doesn't exist");
			return null;
		} catch (SecurityException e){
			System.out.println("Security?");
			return null;
		}
		ByteArrayOutputStream Bout = new ByteArrayOutputStream();
		
		try {
			for (int i; (i = fin.read(buff)) != -1;){
				Bout.write(buff, 0, i);
			}
			fin.close();
		} catch (IOException e){
			System.out.println("Can't read file");
			return null;
		}
		torrentFile = Bout.toByteArray();
		
		//get torrentInfo
		try {
			torrentInfo = new TorrentInfo(torrentFile);
		} catch (BencodingException e){
			System.out.println("Corrupt file.");
			return null;
		} catch (IllegalArgumentException e){
			System.out.println("File is null.");
			return null;
		}
		
		return torrentInfo;
	}
	
	/** Manage interactive IO */
    private BufferedReader inputReader = 
	new BufferedReader(new InputStreamReader(System.in));
    
    /**
     * Display a prompt and read a line of input
     * @return user input
     */
    public String promptedInput() throws IllegalArgumentException{
	System.out.printf(">>> ");
	System.out.flush();
	String result = null;
	try {
	    result = inputReader.readLine();
	} catch (IOException ex) {
	    throw new IllegalArgumentException("IO error.");
	}
	return result;
    }

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		boolean debug = true;
		TorrentInfo torrentInfo = null;
		String outFile = "";
		
		byte[] pID = new byte[20];
		
		System.arraycopy(PID_HEAD, 0, pID, 0, PID_HEAD.length);
		System.arraycopy(new String("PeerIDTestingYeah").getBytes(), 0, pID, PID_HEAD.length, 20 - PID_HEAD.length);
		
		if (args.length != 2){
			printUsage();
			return;
		}
		
		torrentInfo = getInfo(args[0]);
		outFile = args[1];
		
		//make this into exception so turn nulls in getInfo into throws
		if (torrentInfo == null){
			return;
		}
		
		LockedVariables var = new LockedVariables(torrentInfo);
		FileHandler handler = new FileHandler(outFile, var);
		UploadServer server = new UploadServer(torrentInfo, pID, var);
		Thread h = new Thread(handler);
		Thread s = new Thread(server);
		try{
			h.start();
			s.start();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		RUBTClient rc = new RUBTClient();
		rc.printHelp();
		do {
		    try {
				String input = rc.promptedInput().trim();
				if (input == null || input.equalsIgnoreCase("quit")) {
					System.out.println("closing client and saving state");
				    break;
				} else if (input.equals("help")) {
				    rc.printHelp();
				    continue;
				} else if (input.equals("pause")) {
					//interrupt all threads
					if (h.isAlive()){
						h.interrupt();
					}
					if (s.isAlive()){
						s.interrupt();
					}
					//join all threads
					h.join();
					s.join();
				} else if (input.equals("resume")){
					h = new Thread(handler);
					h.start();
					server = new UploadServer(torrentInfo, pID, var);
					s = new Thread(server);
					s.start();
				}
		    } catch (IllegalArgumentException e) {
		    	System.out.println(e);
		    } catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while (true);
		///interrupt all threads;
		if (h.isAlive()){
			if (debug) System.out.println("Inturrupting Handler");
			 h.interrupt();
		}
		if (s.isAlive()){
			s.interrupt();
		}
		//join all threads
		try {
			h.join();
			s.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
