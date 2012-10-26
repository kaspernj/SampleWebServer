import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;


public class SampleWebServer extends Thread{
	private ServerSocket socketSrv;
	private ArrayList<HttpClient> clients = new ArrayList<HttpClient>();
	public HashMap<String, String> opts;
	
	public SampleWebServer(HashMap<String, String> in_opts) throws IOException{
		opts = in_opts;
		socketSrv = new ServerSocket(8081);
	}
	
	public void removeHttpClient(HttpClient hc){
		clients.remove(hc);
	}
	
	public void run(){
		while(true){
			try{
				Socket connSock = socketSrv.accept();
				
				String ip = connSock.getLocalAddress().toString();
				Integer port = connSock.getLocalPort();
				
				System.out.println("Accepted socket connection from IP: " + ip);
				
				BufferedReader ioIn = new BufferedReader(new InputStreamReader(connSock.getInputStream()));
				DataOutputStream ioOut = new DataOutputStream(connSock.getOutputStream());
				
				HttpClient client = new HttpClient(this, connSock, ip, port, ioIn, ioOut);
				clients.add(client);
				
				client.start();
			}catch(Exception e){
				System.err.println("Error while trying to accept new client: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
