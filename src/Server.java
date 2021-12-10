import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Server {
	private static final int PORT = 50001;
	private static int queue = 0;
	private static final int Nmax = 10;
	private static Set<InetAddress> users = new HashSet<InetAddress>();
	public static void main(String[] args) throws IOException, InterruptedException{
		DatagramSocket serverSocket = new DatagramSocket(PORT);
		boolean fl = true;
		System.out.println("Server starts!");
		while(fl) {
			byte[] receivingDataBuffer = new byte[1024];
			DatagramPacket inputPacket =  new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
			serverSocket.receive(inputPacket);
			if(queue < Nmax) {
				Processing processing = new Processing(serverSocket, inputPacket);
				processing.run();
			}
		}
		serverSocket.close();
	}
	
	private static class Processing extends Thread {
		private DatagramSocket serverSocket;
		private DatagramPacket inputPacket;
		private Object obj = new Object();//for synchronizing
		public Processing(DatagramSocket serverSocket, DatagramPacket inputPacket) {
			this.serverSocket = serverSocket;
			this.inputPacket = inputPacket;
		}
		
		@Override
		public void run() {
			InetAddress senderAddress = inputPacket.getAddress();
			int senderPort = inputPacket.getPort();
			users.add(senderAddress);
			byte[] commandBuffer = new byte[1024];
			commandBuffer = inputPacket.getData();
			String commandData = new String(commandBuffer);
			System.out.println(commandData);
			byte[] data = null;
			
			try {
				data = processingCommand(commandData, senderAddress);
			} catch (IOException e) {
				System.out.println("Error with processing command!" + e);
			}
			
			if(data != null) {
				DatagramPacket outputPacket = new DatagramPacket(
						data, data.length,
						senderAddress,senderPort
						);
				
				synchronized (obj) {
					try {
						serverSocket.send(outputPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		 private byte[] processingCommand(String command, InetAddress senderAddress) throws IOException {
        	String data = "";
        	byte[] dataByte;
        	String task = command.substring(0,indexCommand(command));
        	switch(task) {
        	case "ECHO":
        		data = command.substring(4, command.length());
        		data.trim();
        		dataByte = data.getBytes();
        		break;
        	
        	case "TIME":
        		Date date = new Date();
        		data = date.toString();
        		dataByte = data.getBytes();
        		break;
        	
        	case "CLOSE":
        		data = "Goodbay";
        		dataByte = data.getBytes();
				users.remove(senderAddress);
        		break;
        	
        	case "RECONNECT":
        		if(users.contains(senderAddress)) {
        			data = command.substring(13, command.length());
        			int index = Integer.parseInt(command.substring(10, 11));
        			data = data.substring(0, indexCommand(data));
        			String path = "data/" + data;
        			byte[] file = Files.readAllBytes(Paths.get(path));
        			download(file, index);
        			}
    			dataByte = null;
        		break;
        
        	case "DOWNLOAD":
        		data = command.substring(9, command.length());
        		data = data.substring(0, indexCommand(data));
        		String path = "data/" + data;
        		byte[] file = Files.readAllBytes(Paths.get(path));
        		download(file, 0);
        		dataByte = null;
        		break;
        		
        	case "SENDALL":
        		data = command.substring(8, command.length());
        		data = data.substring(0, data.length());
        		sendAll(data.getBytes());
        		dataByte = null;
        	default:
        		data = "Unknown command.";
        		dataByte = data.getBytes();
        		break;
        	}
        	
        	return dataByte;
        }
		 
		private void download(byte[] file, int index) {
			int n = file.length / (64 * 1000);
			List<byte[]> list = new ArrayList<byte[]>();
			for(int i = 0; i < n; i++) {
				byte[] block = new byte[64002];
				block[0] = (byte)n;
				block[1] = (byte)i;
				for(int j = i * 64000; j < i * 64000 + 64000; j++) {
					try{
						block[j - i * 64000 + 2] = file[j];
						}
					catch(Exception e) {
							break;
						}
				}
			}
			for(byte[] block : list) {
				InetAddress senderAddress = inputPacket.getAddress();
				int senderPort = inputPacket.getPort();
				DatagramPacket outputPacket = new DatagramPacket(
						block, block.length,
						senderAddress,senderPort
						);
				
				synchronized (obj) {
					try {
						serverSocket.send(outputPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		 
		private void sendAll(byte[] data) {
			for(InetAddress senderAddress : users) {
				int senderPort = inputPacket.getPort();
				DatagramPacket outputPacket = new DatagramPacket(
						data, data.length,
						senderAddress,senderPort
						);
				
				synchronized (obj) {
					try {
						serverSocket.send(outputPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		 
        private int indexCommand(String s) {
        	int index = 0;
        	
        	for(int i = 0; i < s.length(); i++)
        		if((s.charAt(i) == ' ')||(s.charAt(i) == '\n')||(s.charAt(i) == '\r')||(s.charAt(i) == '\0')) {
        			index = i;
        			break;
        		}
        	
        	return index;
        }
	}
	
}
