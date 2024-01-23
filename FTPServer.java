package ftpServer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

public class FTPServer {
    private static boolean isRunning = true;
    static String currentDirectory = System.getProperty("user.dir");
    private static String rootDirectory = System.getProperty("user.dir");
    static String status=" ";
    private static Stack<String> directoryStack = new Stack<>();
    private static final String SECRET_KEY = "abcdefgh12345678";
    		
   
    private static final String ALGORITHM = "AES";
    
    
    public static void main(String[] args) {
        int port = 21; // FTP default port
        int accept_cmd=0;
         
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("FTP Server is listening on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                
                if (authenticateClient(clientSocket)) {
                    
                		handleClient(clientSocket);
                } else {
        
                    clientSocket.close();
                    System.out.println("Client authentication failed. Disconnecting.");
                }
            }

            
            serverSocket.close();
            System.out.println("Server shutdown");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    private static boolean authenticateClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

         
            writer.println("ftp-server>220 Enter username:");
            String username = reader.readLine();
            System.out.println("Username: " + username);

           
            writer.println("ftp-server>331 Enter password:");
            String password = reader.readLine();
            System.out.println("Password: " + password);

          
            if (isValidCredentials(username, password)) {
                writer.println("230 Authentication successful. Welcome!");
                return true;
            
            } else {
            
            	if(doesUserExist(username)) {
            	writer.println("201 Error: Wrong password. Press enter to return...");
            	return false;
            	}else {
            		
            		
                    writer.println("201 User not found. Do you want to create a new user? (y/n)");
                    String createNewUserResponse = reader.readLine();

                    if ("y".equalsIgnoreCase(createNewUserResponse)) {
                        createNewUser(clientSocket, writer);
                    } else {
                        writer.println("221 Goodbye. Press enter to return...");
                        clientSocket.close();
                       
                        System.out.println("Client disconnected");
                       
                    }

                    return false;
            	}
             
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    static boolean doesUserExist(String username) {
        try {
            File credentialsFile = new File("credentials.txt");
            Scanner scanner = new Scanner(credentialsFile);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String decrypted_data=decrypt(line);
                String[] parts = decrypted_data.split("_");
                if (parts.length == 2) {
                    String storedUsername = parts[0];
                    if (username.equals(storedUsername)) {
                        scanner.close();
                        return true;
                    }
                }
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static boolean isValidCredentials(String username, String password) {
        try {
            List<String> lines = Files.readAllLines(Paths.get("credentials.txt"));
            for (String line : lines) {
            	String decrypted_data=decrypt(line);
                String[] parts = decrypted_data.split("_");
                if (parts.length == 2) {
                    String storedUsername = parts[0];
                    String storedPassword = parts[1];
                    if (username.equals(storedUsername) && password.equals(storedPassword)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void createNewUser(Socket clientSocket, PrintWriter writer) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            writer.println("ftp-server>220 Enter new username:");
            String newUsername = reader.readLine();
            System.out.println("New Username: " + newUsername);

            writer.println("ftp-server>331 Enter new password:");
            String newPassword = reader.readLine();
            System.out.println("New Password: " + newPassword);
            if(doesUserExist(newUsername)) {
            	 writer.println("240 Error: user already exist");
            	 return;
            }
           
            try (BufferedWriter out = new BufferedWriter(new FileWriter("credentials.txt", true))) {
            	String data=encrypt(newUsername + "_" + newPassword);
          
            	out.write(data);
                out.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            writer.println("230 New user created. Welcome!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String encrypt(String data) {
        try {
            Key key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            Key key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Key generateKey() {
        return new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
    }

    

    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            String command;
            do {
            
                command = reader.readLine();
          
                System.out.println("Received command: " + command);

                if (command != null) {
                  
                    if (command.toUpperCase().equals("QUIT")) {
                        writer.println("221 Goodbye.");
                        break; // Exit the loop to disconnect the client
                    }else if(command.toUpperCase().equals("STOP-SERVER")) {
                    	writer.println("221 Server shutdown");
                    	isRunning=false;
                    	break;
                    	
                    }  else if (command.toUpperCase().startsWith("MKDIR")) {
                        handleMkdirCommand(command, writer);
                    } 
                    
                    else if (command.toUpperCase().startsWith("CD")) {
                        handleCdCommand(command, writer);
                        
                    } else if (command.toUpperCase().startsWith("RMDIR")) {
                        handleRmdirCommand(command, writer);
                        
                    } else if (command.toUpperCase().equals("DIR")) {
                        handleDirCommand(writer);
                    }
    
                    else {
                  
                        writer.println("500 Unknown command: " + command);
                    }
                }
            } while (!command.toUpperCase().equals("QUIT"));

            
            clientSocket.close();
            System.out.println("Client disconnected");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void handleMkdirCommand(String command, PrintWriter writer) {
        
        String[] commandParts = command.split(" ");
        if (commandParts.length == 2) {
            String directoryName = commandParts[1];

           
            try {
                Files.createDirectories(Paths.get(currentDirectory, directoryName));
                writer.println("257 \"" + directoryName + "\" directory created.");
            } catch (IOException e) {
                writer.println("550 Requested action not taken. Directory creation failed.");
            }
        } else {
            writer.println("501 Syntax error in parameters or arguments.");
        }
    }
    private static void handleCdCommand(String command, PrintWriter writer) {
        
        String[] commandParts = command.split(" ");
        if (commandParts.length == 2) {
            String directoryName = commandParts[1];

            
            if (directoryName.equals("/") || directoryName.equals("\\") || directoryName.equals("~")) {
                currentDirectory = rootDirectory;
                writer.println("250 Directory changed to root: \"" + currentDirectory + "\".");
                return;
            }

            
            if (directoryName.equals("..") && !directoryStack.isEmpty()) {
                currentDirectory = directoryStack.pop();
                writer.println("250 Directory changed to previous: \"" + currentDirectory + "\".");
                return;
            }

            
            directoryStack.push(currentDirectory);

            // Change the current directory
            Path newDirectory = Paths.get(currentDirectory, directoryName);
            if (Files.exists(newDirectory) && Files.isDirectory(newDirectory)) {
                currentDirectory = newDirectory.toString();
                writer.println("250 Directory changed to \"" + currentDirectory + "\".");
            } else {
                writer.println("550 Requested action not taken. Directory does not exist.");
            }
        } else {
            writer.println("501 Syntax error in parameters or arguments.");
        }
    }
    private static void handleRmdirCommand(String command, PrintWriter writer) {
       
        String[] commandParts = command.split(" ");
        if (commandParts.length == 2) {
            String directoryName = commandParts[1];

           
            Path targetDirectory = Paths.get(currentDirectory, directoryName);
            try {
                if (Files.exists(targetDirectory) && Files.isDirectory(targetDirectory)) {
                    Files.walk(targetDirectory)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);

                    writer.println("250 \"" + targetDirectory + "\" directory removed.");
                } else {
                    writer.println("550 Requested action not taken. Directory does not exist.");
                }
            } catch (IOException e) {
                writer.println("550 Requested action not taken. Directory removal failed.");
            }
        } else {
            writer.println("501 Syntax error in parameters or arguments.");
        }
    }
    

    private static void handleDirCommand(PrintWriter writer) {
        try {
            
            File currentDir = new File(currentDirectory);
            File[] files = currentDir.listFiles();

            if (files != null) {
                StringBuilder directoryListing = new StringBuilder();

                for (File file : files) {
                    if (file.isDirectory()) {
                        directoryListing.append("DIR  ").append(file.getName()).append(" ");
                    } else {
                        directoryListing.append("FILE ").append(file.getName()).append(" ");
                    }
                }

            
                writer.print(directoryListing.toString());
             
                writer.flush();
            }

            writer.println("  >>>226 Directory listing completed.");
        } catch (SecurityException e) {
            writer.println("550 Requested action not taken. Permission denied.");
        }
    }

    
    
    
}

    
    
    

