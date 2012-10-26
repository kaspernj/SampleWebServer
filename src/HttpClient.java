import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;


public class HttpClient extends Thread {
	Socket connSock;
	String ip;
	Integer port;
	BufferedReader ioIn;
	DataOutputStream ioOut;
	String mode;
	String url;
	String filePath;
	String httpVersion;
	HashMap<String,String> headersSend = new HashMap<String, String>();
	Pattern patternStatusLine = Pattern.compile("^(GET|POST)\\s+(.+)\\s+HTTP\\/([\\d\\.]+)$");
	Pattern patternHeaders = Pattern.compile("^(.+):\\s*(.+)$");
	Integer contentLength;
	Boolean contentLengthGiven;
	HashMap<String, String> headers = new HashMap<String, String>();
	SampleWebServer sws;
	ArrayList<String> acceptEncodings = new ArrayList<String>();
	FileInputStream fstream;
	DataInputStream in;
	BufferedReader br;
	
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
				System.out.println("Waiting for new request.");
				
				//Reset various variables.
				contentLengthGiven = false;
				contentLength = 0;
				mode = "headers";
				headers.clear();
				acceptEncodings.clear();
				headersSend.clear();
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
					httpVersion = matcherStatusLine.group(3).toLowerCase().trim();
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
				
				//Set default URL if empty.
				if (url.equals("/")){
					url = "/index.html";
				}
				
				//Fill the 'acceptEncodings' ArrayList.
				if (headers.containsKey("accept-encoding")){
					String[] encodingsList = headers.get("accept-encoding").split("\\s*,\\s*");
					for(String item: encodingsList){
						acceptEncodings.add(item);
					}
				}
				
				//Figure out full file-path.
				filePath = sws.opts.get("DocumentRoot") + url;
				System.out.println("Serve URL: " + url);
				System.out.println("Serve filepath: " + filePath);
				
				//FIXME: Content-Type should by dynamic based on the file we are sending.
				headersSend.put("Content-Type", "text/html");
				
				fstream = new FileInputStream(filePath);
				in = new DataInputStream(fstream);
				br = new BufferedReader(new InputStreamReader(in));
				
				//Figure out if GZip compression should be used.
				if (acceptEncodings.contains("gzip")){
					System.out.println("Using GZip compression for this request.");
					
					File tempfileFile = File.createTempFile("gzip_outpout", ".temp");
					FileOutputStream tempfile = new FileOutputStream(tempfileFile.getAbsolutePath());
					OutputStreamWriter gzipOut = new OutputStreamWriter(new GZIPOutputStream(tempfile));
					
					String line;
					while((line = br.readLine()) != null){
						gzipOut.write(line);
						gzipOut.write("\r\n");
					}
					
					gzipOut.close();
					
					headersSend.put("Content-Encoding", "gzip");
					headersSend.put("Content-Length", String.valueOf(tempfileFile.length()));
					
					//Close previous reader and open a new one for the GZip'ed file.
					br.close();
					br = new BufferedReader(new InputStreamReader(new FileInputStream(tempfileFile.getAbsolutePath())));
					
					filePath = tempfileFile.getAbsolutePath();
				}else{
					//Use chunked encoding for HTTP 1.1 without GZip.
					if (httpVersion.equals("1.1")){
						headersSend.put("Transfer-Encoding", "chunked");
					}
					
					System.out.println("Wont use any special encoding for this request (" + acceptEncodings.toString() + ").");
				}
				
				//Run the right serve-method according to the HTTP-version.
				if (httpVersion.equals("1.1")){
					serveHttp11Request();
				}else{
					throw new Exception("Dont know how to handle that HTTP-version: " + httpVersion);
				}
				
				System.out.println("Done serving the request.");
				
				br.close();
				in.close();
				fstream.close();
				
				closeStream();
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
		String line;
		ioOut.writeBytes("HTTP/1.1 200 OK\r\n");
		
		headersSend.put("Connection", "Keep-Alive");
		headersSend.put("Keep-Alive", "timeout=15, max=30");
		
		for(String key: headersSend.keySet()){
			String val = headersSend.get(key);
			ioOut.writeBytes(key + ": " + val + "\r\n");
			System.out.println("Sent header: " + key + ": " + val);
		}
		
		ioOut.writeBytes("\r\n");
		ioOut.flush();
		
		if (headersSend.containsKey("Transfer-Encoding") && headersSend.get("Transfer-Encoding") == "chunked"){
			while((line = br.readLine()) != null){
				line += "\r\n";
				Integer length = line.length();
				String length_str = Integer.toString(length, 16);
				
				write(length_str + "\r\n");
				write(line);
				write("\r\n");
				write("\r\n");
				
				System.out.println("Sent a piece of the body: '" + length_str + "\r\n" + line + "\r\n'.");
			}
			
			write("0\r\n");
		}else if(headersSend.containsKey("Content-Length")){
			byte[] result = Files.readAllBytes(Paths.get(filePath));
			ioOut.write(result);
			System.out.println("Sent a piece of the body not-chunked (" + String.valueOf(result.length) + ").");
		}
		
		ioOut.flush();
		System.out.println("Done with the request.");
	}
	
	//Writes a string to the correct object (if using GZip compression or not).
	public void write(String str) throws IOException{
		ioOut.writeBytes(str);
	}
	
	//This should be called at the end of a request to close any hanging streams like the GZip stream.
	public void closeStream() throws IOException{
		
	}
}
