import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class SampleWebServer {
	private ServerSocket socketSrv;
	
	public SampleWebServer() throws IOException{
		socketSrv = new ServerSocket(8081);
		ArrayList<HttpClient> clients = new ArrayList<HttpClient>();
		
		while(true){
			Socket connSock = socketSrv.accept();
			
			String ip = connSock.getLocalAddress().toString();
			Integer port = connSock.getLocalPort();
			
			System.out.println("Accepted socket connection from IP: " + ip);
			
			BufferedReader ioIn = new BufferedReader(new InputStreamReader(connSock.getInputStream()));
			DataOutputStream ioOut = new DataOutputStream(connSock.getOutputStream());
			
			HttpClient client = new HttpClient(this, connSock, ip, port, ioIn, ioOut);
			clients.add(client);
			
			client.start();
		}
	}
}
