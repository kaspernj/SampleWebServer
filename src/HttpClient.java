import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HttpClient extends Thread {
	Socket connSock;
	String ip;
	Integer port;
	BufferedReader ioIn;
	DataOutputStream ioOut;
	String mode;
	String url;
	String filePath;
	Pattern patternStatusLine = Pattern.compile("^(GET|POST)\\s+(.+)\\s+HTTP\\/([\\d\\.]+)$");
	Pattern patternHeaders = Pattern.compile("^(.+):\\s*(.+)$");
	Integer contentLength;
	Boolean contentLengthGiven;
	HashMap<String, String> headers = new HashMap<String, String>();
	SampleWebServer sws;
	
	public HttpClient(SampleWebServer in_sws, Socket in_connSock, String in_ip, Integer in_port, BufferedReader in_ioIn, DataOutputStream in_ioOut){
		sws = in_sws;
		connSock = in_connSock;
		ip = in_ip;
		port = in_port;
		ioIn = in_ioIn;
		ioOut = in_ioOut;
	}
	
	/** Accepts new HTTP-request from the input. */
	public void run(){
		try{
			//Read requests until client disconnects.
			while(true){
				//Reset various variables.
				contentLengthGiven = false;
				contentLength = 0;
				mode = "headers";
				headers.clear();
				String requestMethod = "";
				String postdata = "";
				
				
				//Read and match request line.
				String requestLine = ioIn.readLine();
				if (requestLine == null){
					System.out.println("Client has disconnected - closing HttpClient.");
					break;
				}
				
				Matcher matcherStatusLine = patternStatusLine.matcher(requestLine);
				
				if (matcherStatusLine.find()){
					requestMethod = matcherStatusLine.group(1).toLowerCase().trim();
					url = matcherStatusLine.group(2).trim();
				}else{
					throw new Exception("Could not match the request-line: " + requestLine);
				}
				
				while(true){
					String line = ioIn.readLine();
					
					if (requestMethod.equals("post") && mode.equals("body")){
						postdata += line;
					}else if (mode.equals("headers") && line.equals("")){
						if (requestMethod.equals("get")){
							break;
						}else if(requestMethod.equals("post")){
							throw new Exception("Now we should read the post-data.");
						}
					}else if(mode.equals("headers")){
						Matcher matcherHeaders = patternHeaders.matcher(line);
						if (matcherHeaders.find()){
							String key = matcherHeaders.group(1).toLowerCase().trim();
							String val = matcherHeaders.group(2);
							
							if (key.equals("content-length")){
								contentLength = Integer.parseInt(val);
							}
							
							System.out.println("New header: " + key + ": " + val);
							headers.put(key, val);
						}else{
							throw new Exception("Could not match the line as a header: '" + line + "'.");
						}
					}else{
						throw new Exception("Dont know what to do with that line: '" + line + "'.");
					}
				}
				
				if (url.equals("/")){
					url = "/index.html";
				}
				
				filePath = sws.opts.get("DocumentRoot") + url;
				System.out.println("Serve URL: " + url);
				System.out.println("Serve filepath: " + filePath);
				
				serveHttp11Request();
				
				System.out.println("Done serving the request.");
			}
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}finally{
			System.out.println("Finally for HttpClient called - removing from array.");
			sws.removeHttpClient(this);
		}
	}
	
	/** Sends the request as HTTP 1.1. */
	public void serveHttp11Request() throws IOException{
		FileInputStream fstream = new FileInputStream(filePath);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;
		
		ioOut.writeBytes("HTTP/1.1 200 OK\r\n");
		
		HashMap<String,String> headersSend = new HashMap<String, String>();
		headersSend.put("Content-Type", "text/html");
		headersSend.put("Connection", "Keep-Alive");
		headersSend.put("Keep-Alive", "timeout=15, max=30");
		headersSend.put("Transfer-Encoding", "chunked");
		
		for(String key: headersSend.keySet()){
			String val = headersSend.get(key);
			ioOut.writeBytes(key + ": " + val + "\r\n");
			System.out.println("Sent header: " + key + ": " + val);
		}
		
		ioOut.writeBytes("\r\n");
		
		while((line = br.readLine()) != null){
			line += "\r\n";
			Integer length = line.length();
			String length_str = Integer.toString(length, 16);
			
			ioOut.writeBytes(length_str + "\r\n");
			ioOut.writeBytes(line + "\r\n");
			
			System.out.println("Sent a piece of the body: '" + length_str + "\r\n" + line + "\r\n'.");
		}
		
		ioOut.writeBytes("0\r\n");
		ioOut.writeBytes("\r\n");
		
		System.out.println("Done with the request.");
	}
}
