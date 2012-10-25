import java.io.BufferedReader;
import java.io.DataOutputStream;
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
			//Reset various variables.
			contentLengthGiven = false;
			contentLength = 0;
			mode = "headers";
			headers.clear();
			String requestMethod = "";
			String url = "";
			String postdata = "";
			
			
			//Read and match request line.
			String requestLine = ioIn.readLine();
			Matcher matcherStatusLine = patternStatusLine.matcher(requestLine);
			
			if (matcherStatusLine.find()){
				requestMethod = matcherStatusLine.group(1).toLowerCase().trim();
				url = matcherStatusLine.group(2);
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
			
			System.out.println("Serve URL: " + url);
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
