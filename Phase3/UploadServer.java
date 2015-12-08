import java.io.IOException;
import java.net.ServerSocket;


public class UploadServer implements Runnable {
	ServerSocket s = null;
	int port = 6881;
	int maxport = 6889;
	String name = "UploadServer";
	boolean opened = false;
	boolean running = true;
	//keep track of the number of download peers opened and visited
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		//open a ServerSocket
		for (; port <= maxport && !opened; port++){
			try {
				s = new ServerSocket(port);
				opened = true;
				//start tracker
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
		
		/*while (running){
			try {
				//check for upload pool capacity
				s.accept();
				//create a new upload thread
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		if (!s.isClosed()){
			try {
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
