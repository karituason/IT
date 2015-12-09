import java.io.*;
import java.lang.Exception;
import java.util.StringTokenizer;

public class FileHandler implements Runnable{
	boolean debug = false;
	boolean debug2 = true;
	private volatile LockedVariables var;
	private String filename;
	private String tempFilename;
	private RandomAccessFile outfile = null;
	private File file;
	private File tmpFile;
	private String name = "FileHandler";
	public FileHandler (String filename, LockedVariables var){
		if (filename == null || filename.equals("") || var == null) {
			throw new IllegalArgumentException("filename or var cannot be null or empty");
		}
		this.filename = filename;
		this.tempFilename = filename + ".txt";
		this.var = var;
		this.file = null;
		this.tmpFile = null;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		boolean first = true;
		boolean completed = false;
		if (debug || debug2) System.out.println(name + " starting");
		boolean already_exists = false;
		try {
			already_exists = fileExists();
			synchronized(var){
				var.filed = true;
				if (var.left == 0){
					completed = true;
				}
			}
			if (Thread.interrupted()){
				if (debug2) System.out.println(name +": interrupted");
				return;
			}
			
			if (already_exists){
				if (debug2) System.out.println(name + " Exists");
			} else {
				if (debug2) System.out.println(tempFilename + " doesn't exist");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PrintWriter pout = null;
		if (! completed){
		try {
			outfile = new RandomAccessFile(filename, "rws");
			if (debug2) System.out.println("opened outfile");
			if (already_exists){
				pout = new PrintWriter(new BufferedWriter(new FileWriter(tempFilename, true)));
				if (debug2) System.out.println("append");
			} else {
				pout = new PrintWriter(new BufferedWriter(new FileWriter(tempFilename, false)));
				if (debug2) System.out.println("overwrite");
			}
			if (debug) System.out.println("put in pout");
		} catch (Exception e){
			e.printStackTrace();
		}
		if (pout != null && outfile != null){
			if (Thread.interrupted()){
				if (debug2) System.out.println(name + ": interrupted");
				completed = true;
			}
			
			while (!completed){
				if (Thread.interrupted()){
					if (debug2) System.out.println(name + ": interrupted");
					completed = true;
					continue;
				}
				if (debug) System.out.println(name + ":waiting on var");
				synchronized(var){
					if (debug) System.out.println(name + ": have var");
					if (debug) System.out.println(name + " in Sync");
					int index = -1;
					while ((index = var.getFileIndex()) != -1){
						try {
							if (debug) System.out.println(name + ": In while for stack");
							if (debug) System.out.println (name + ": index is "+ index);
							byte[] piece = var.pieces[index];
							if (piece == null){
								if (debug) System.out.println(name + ": piece is null!");
							}
							var.downloaded += piece.length;
							var.left -= piece.length;
							if (var.left == 0){
								completed = true;
							}
							outfile.seek(index * var.piece_size);
							outfile.write(piece);
							if (!already_exists && first){
								pout.print("" + index);
								first = false;
							} else {
								pout.println(" " + index);
							}
						} catch(IOException e){
							e.printStackTrace();
						}
					}
				}
			}
			try {
				//close files
				outfile.close();
				pout.close();
			} catch (Exception e){
				System.out.println("files were already closed");
			}
		}
		}
			System.out.println("Closing File Handler");
			
	}
	
	public boolean fileExists() throws IOException{
		if (Thread.interrupted()){
			Thread.currentThread().interrupt();
			return false;
		}
		if (debug) System.out.println("in FileExists");
		BufferedReader tempfile = null;
		
		try{
			file = new File(filename);
			tmpFile = new File(tempFilename);
		} catch (NullPointerException e){
			return false;
		}
		
		try {
			outfile = new RandomAccessFile(file, "r");
			tempfile = new BufferedReader(new FileReader(tmpFile));
		} catch (FileNotFoundException e){
			return false;
		} catch (SecurityException e){
			return false;
		} catch (IllegalArgumentException e){
			return false;
		}
		synchronized (var){
			int piece_size = var.piece_size;
			int last_size = var.file_size % var.piece_size == 0? var.piece_size : var.file_size % var.piece_size;
			byte[] piece;
			String line = "";
			int index;
			try{
				
				while((line = tempfile.readLine()) != null){
					if (debug) System.out.println ("in while");
					if (debug) System.out.println(line);
					StringTokenizer st = new StringTokenizer(line);
					while (st.hasMoreTokens()){
						if (debug) System.out.println("in tokens");
						index = Integer.parseInt(st.nextToken());
						if (debug) System.out.println(index);
						var.piece_status[index] = 1;
						if (index == (var.piece_status.length -1)){
							piece = new byte[last_size];
						} else {
							piece = new byte[piece_size];
						}
						outfile.seek(index * piece_size);
						outfile.readFully(piece);
						var.left -= piece.length;
					}
				}
			} catch (Exception e){
				return false;
			} finally {
				tempfile.close();
			}
		}
		
		if (Thread.interrupted()){
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}
	
}
