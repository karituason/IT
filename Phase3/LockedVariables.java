import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Stack;

import GivenTools.TorrentInfo;

public class LockedVariables{
	class Index_count{
		int index;
		int count;
		public Index_count(int index, int count){
			this.index = index;
			this.count = count;
		}		
	}
	
	boolean debug = false;
	byte[][] pieces;
	int[] piece_status; //0 = don't have; 1 = have;
	int[] piece_count;
	PriorityQueue<Index_count> toDownload;
	int uploaded;
	int downloaded;
	int trackerState;
	int left;
	private Stack<Integer> updated;
	int piece_size;	
	int file_size;
	public final ByteBuffer[] piece_hashes;

	public LockedVariables(TorrentInfo torrent_info){
		int number_of_pieces = torrent_info.piece_hashes.length;
		pieces = new byte[number_of_pieces][];
		piece_status = new int[number_of_pieces];
		piece_count = new int[number_of_pieces];
		uploaded = 0;
		downloaded = 0;
		left = torrent_info.file_length;
		toDownload = new PriorityQueue<Index_count>(myCompare);
		updated = new Stack<Integer>();
		piece_size = torrent_info.piece_length;	
		file_size = torrent_info.file_length;
		this.piece_hashes = torrent_info.piece_hashes;
	}
	public boolean needMore(){
		return left == 0;
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
	
	public void processHave(int index){
		if (piece_status[index] == 0){
			Index_count temp = new Index_count(index, piece_count[index]);
			piece_count[index]++;
			
			toDownload.remove(temp);
			temp.count++;
			toDownload.add(temp);
		}
	}
	
	public int seeNext(){
		Index_count temp = toDownload.peek();
		if (temp != null){
			return temp.index;
		} else {
			return -1;
		}
	}
	
	public void take(){
		toDownload.poll();
	}
	
	public void removeIndex(int index){
		Index_count temp = new Index_count(index, piece_count[index]);
		toDownload.remove(temp);
	}
	
	Comparator<Index_count> myCompare = new Comparator<Index_count>(){
		public int compare(Index_count o1, Index_count o2) {
			// TODO Auto-generated method stub
			return Integer.compare(o1.count, o2.count);
		}
	};
}
