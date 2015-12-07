import java.lang.Object;
import java.util.concurrent.locks.*;
import java.util.Stack;
import java.io.*;

import GivenTools.TorrentInfo;

public class LockedVariables{
	boolean debug = false;
	byte[][] pieces;
	int[] piece_status; //0 = don't have; 1 = downloaded; 2 = downloading
	int uploaded;
	int downloaded;
	int left;
	private Stack<Integer> updated;
	private final ReentrantLock lock = new ReentrantLock();
	int piece_size;	
	int file_size;

	public LockedVariables(TorrentInfo torrent_info){
		int number_of_pieces = torrent_info.piece_hashes.length;
		pieces = new byte[number_of_pieces][];
		piece_status = new int[number_of_pieces];
		uploaded = 0;
		downloaded = 0;
		left = torrent_info.file_length;
		updated = new Stack<Integer>();
		piece_size = torrent_info.piece_length;	
		file_size = torrent_info.file_length;
	}	

	public void enter(){
		while (!lock.tryLock()){
			//
		}	
	}

	public void leave(){
		lock.unlock();	
	}

	public int getFileIndex(){
		if (!updated.empty()){
			return updated.pop();
		} else {
			return -1;
		}
	}

	public void addFileIndex(int index){
		if (debug) System.out.println("Variables: adding ["+ index +"] to stack");
		updated.push(index);
	}
}
