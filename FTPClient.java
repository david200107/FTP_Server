package ftpServer;

import java.io.*;
import java.util.Scanner;
import java.net.*;

public class FTPClient {
    public static void main(String[] args) {
        // Get the hostname from the user
     
        String cmd="";
        Scanner scan=new Scanner(System.in);
        Console console=System.console();
        String command=null;
        String response;
        int port = 21; // FTP default port
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        
        String arg[];
        while(true) {
        while(true) {
        while(true) {

        	System.out.print("client> ");
        	try {
				cmd=consoleReader.readLine();
				
				if(cmd.startsWith("ftp"))
					break;
				System.out.print("Error: Invalid command\nTry cmd: ftp server_address\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	
        }
        	
     
     
        arg=cmd.split(" ");
        
        if((arg[1].equals("localhost"))||(arg[1].equals("127.0.0.1"))||arg[1].equals("192.168.1.11"))
        	break;
        System.out.print("Error: Invalid server address\n");
        
        }
        
        String serverAddress = arg[1];
       
        do {
        try {
            Socket socket = new Socket(serverAddress, port);
            System.out.println("Connected to the FTP server");

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
           

            String userRequest = getServerResponse(reader);
            System.out.print(userRequest);
            String username = consoleReader.readLine();
           writer.println(username);

            String passwordRequest = getServerResponse(reader);
            System.out.print(passwordRequest);
            String password = consoleReader.readLine();
            writer.println(password);
         
            String authResponse = reader.readLine();
            System.out.println(authResponse);
            
           
            if (authResponse.startsWith("230")) {
        
               
                
                do {
                	System.out.print("@"+username+">");
                    command = consoleReader.readLine();

                   
                    writer.println(command);

                    
                     response = reader.readLine();
                    System.out.println(response);
                    
                    if(command.toUpperCase().equals("STOP-SERVER"))
                    	break;

                } while (!command.toUpperCase().equals("QUIT"));
            }else if(authResponse.startsWith("201")) {
            	command=consoleReader.readLine();
            	writer.println(command);
            	response = reader.readLine();
                System.out.println(response);
            	command=consoleReader.readLine();
            	writer.println(command);
            	response = reader.readLine();
                System.out.println(response);
                command=consoleReader.readLine();
            	writer.println(command);
            	response = reader.readLine();
                System.out.println(response);
            
            	
            }else {
            socket.close();
            
             System.out.println("Disconnected from the FTP server");
            // break;
            		
            }
            
            // Close the socket when done
         
        
        } catch (IOException e) {
            //e.printStackTrace();
            break;
        }
        
        }while(!command.toUpperCase().equals("QUIT"));
        
    }
        }
    
   static String getServerResponse(BufferedReader br) {
    	
    	try {
			return br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "Error: Server no response\n";
		}
    }
   static void sendCommandToServer(String cmd,PrintWriter pw) {
	   pw.println(cmd);
   }
   
   
  
}

