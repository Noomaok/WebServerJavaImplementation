///A Simple Web Server (WebServer.java)

package http.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 * 
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 * 
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {

	Socket remote;
	PrintWriter printer;
	BufferedReader bufReader;

	final String DEFAULT_PAGE = "index.html";
	final String RES_FOLDER = "../res";

	/**
	 * WebServer constructor.
	 */
	protected void start() {
		ServerSocket s;

		System.out.println("Webserver starting up on port 3000");
		System.out.println("(press ctrl-c to exit)");
		try {
			// create the main server socket
			s = new ServerSocket(3000);
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}

		while(true) {
			try {
				// wait for a connection
				remote = s.accept();
				// remote is now the connected socket
				bufReader = new BufferedReader(new InputStreamReader(remote.getInputStream()));
				printer = new PrintWriter(remote.getOutputStream());

				String httpMethod = bufReader.readLine();
				HashMap<String,String> headers = new HashMap<>();
				String line = bufReader.readLine();
				while(!line.equals("")) {
					String[] lsplit = line.split(":",0);
					headers.put(lsplit[0].toLowerCase(), lsplit[1]);
					line = bufReader.readLine();
				}
				analyseHttpRequest(httpMethod, headers);
				bufReader.close();
				printer.close();
				remote.close();
			} catch (Exception e) {
				//System.out.println("Error: " + e);
				e.printStackTrace();
			}
		}
	}

	public void analyseHttpRequest(String httpRequest, HashMap<String,String> headers) throws IOException {
		String[] request = httpRequest.split(" ");
		String method = request[0];
		String filename = request[1];
		String httpformat = request[2];

		System.out.println(httpRequest);

		if(filename.equals("/")) filename += DEFAULT_PAGE;
		File fileRequested = new File(RES_FOLDER + filename);
		HashMap<String,String> headersToSend = new HashMap<>();
		
		if(!fileRequested.exists()) {
			headersToSend.put("Content-type", "text/html");
			sendHeader(httpformat, "404 NOK", headersToSend);
			sendFile(new File(RES_FOLDER + "/error.html"));
		} else {
			headersToSend.put("Content-type", getTypeOfFile(fileRequested));
			switch (method) {
				case "GET":
					sendHeader(httpformat, "200 OK", headersToSend);
					sendFile(fileRequested);
					break;
				case "POST":
					if(headers.containsKey("content-length")) {
						if(headers.get("content-type").contains("multipart/form-data")) {
							char[] body = readBody(Integer.parseInt(headers.get("content-length").replaceAll(" ", ""))-6);
							String boundary = headers.get("content-type").split("=", 0)[1];
							String file = new String(body);
							file = file.replace("--"+boundary, "");
							String[] lines = file.split(System.lineSeparator());
							int i = 1;
							String fileSendName = "/newFile";
							while(!lines[i].equals("\r")) {
								if(lines[i].contains("Content-Disposition")) {
									fileSendName = lines[i].substring(lines[i].indexOf("filename=\"")+10, lines[i].lastIndexOf("\""));
								}
								i++;
							}
							String fileSendPath = RES_FOLDER + "/" + fileSendName;
							BufferedWriter writer = new BufferedWriter(new FileWriter(fileSendPath));
							while(i < lines.length) {
								writer.write(lines[i]);
								writer.write(System.lineSeparator());
								i++;
							}
							writer.close();
							sendHeader(httpformat, "200 OK", headersToSend);
							sendFile(fileRequested);
						}
						else {
							char[] body = readBody(Integer.parseInt(headers.get("content-length").replaceAll(" ", "")));
							System.out.println(new String(body));
							sendHeader(httpformat, "200 OK", headersToSend);
							sendFile(fileRequested);
						}
						
					} else {
						headersToSend.put("Content-type", "text/html");
						sendHeader(httpformat, "400 BAD REQUEST", headersToSend);
						sendFile(new File(RES_FOLDER + "/error.html"));
					}
					break;
				case "HEAD":
					sendHeader(httpformat, "200 OK", headersToSend);
					break;
				case "DELETE":
					if(fileRequested.delete()){
						sendHeader(httpformat, "200 OK", headersToSend);
					}
				default:
					break;
			}
		}
	}

	public String getTypeOfFile(File file) {
		String ext = file.getName().split("\\.",0)[1];
		String type;
		switch (ext) {
			case "jpg":
				type = "image/jpeg";
				break;
			case "png":
				type = "image/png";
				break;
			case "gif":
				type = "image/gif";
				break;
			case "html":
				type = "text/html; charset=UTF-8";
				break;
			case "css":
				type = "text/css; charset=UTF-8";
				break;
			case "js":
				type = "application/javascript";
				break;
			case "mp4":
				type = "video/mp4";
				break;
			default:
				type = "";
				break;
		}
		return type;
	}

	public char[] readBody(int length) throws IOException{
		char[] body = new char[length];
		for(int i = 0; i < length; i++) {
			body[i] = (char)bufReader.read();
		}
		return body;
	}

	public void sendHeader(String httpformat, String httpSignal, HashMap<String,String> headers) {
		printer.println(httpformat + " " + httpSignal);
		for(String key: headers.keySet()) {
			printer.println(key + ":" + headers.get(key));
		}
		printer.println();
		printer.flush();
	}

	public void sendFile(File fileToSend) throws IOException {
		ByteArrayOutputStream dataout = new ByteArrayOutputStream();
		dataout.write(Files.readAllBytes(fileToSend.toPath()));
		dataout.writeTo(remote.getOutputStream());
		dataout.flush();
	}

	/**
	 * Start the application.
	 * 
	 * @param args Command line parameters are not used.
	 */
	public static void main(String args[]) {
		WebServer ws = new WebServer();
		ws.start();
	}
}
