// This package statement declares that this file belongs to the 'p2p.service' package
package p2p.service;

// Import the UploadUtils class which contains utility methods for generating codes
import p2p.utils.UploadUtils;

// Import classes for file input/output operations
import java.io.*;
// Import classes for network communication (server and client sockets)
import java.net.ServerSocket;
import java.net.Socket;
// Import HashMap to store port-to-filepath mappings
import java.util.HashMap;

// Define the FileSharer class, which manages file sharing between peers
public class FileSharer {

    // Declare a HashMap to store available files with their associated ports
    // Key: port number (Integer), Value: file path (String)
    private HashMap<Integer, String> availableFiles;

    // Constructor for FileSharer class
    public FileSharer() {
        // Initialize the HashMap to store file-port mappings
        availableFiles = new HashMap<>();
    }

    // Method to offer a file for sharing and get a unique port number
    public int offerFile(String filePath) {
        // Declare a variable to store the generated port number
        int port;
        // Keep trying until we find an available port
        while (true) {
            // Generate a random port number using the utility method
            port = UploadUtils.generateCode();
            // Check if this port is not already in use
            if (!availableFiles.containsKey(port)) {
                // Store the file path with this port number
                availableFiles.put(port, filePath);
                // Return the port number for this file
                return port;
            }
            // If port is already in use, continue the loop to try another port
        }
    }

    // Method to start a file server on a specific port to serve a file
    public void startFileServer(int port) {
        // Get the file path associated with this port number
        String filePath = availableFiles.get(port);
        // If no file is associated with this port, print error and return
        if (filePath == null) {
            System.err.println("No file associated with port: " + port);
            return;
        }

        // Try to create a server socket on the specified port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Print a message showing which file is being served on which port
            System.out.println("Serving file '" + new File(filePath).getName() + "' on port " + port);
            // Wait for a client to connect and accept the connection
            Socket clientSocket = serverSocket.accept();
            // Print the IP address of the connected client
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            // Create a new thread to handle sending the file to the client
            // This allows the server to handle multiple clients if needed
            new Thread(new FileSenderHandler(clientSocket, filePath)).start();

        } catch (IOException e) {
            // Print an error message if there's a problem starting the server
            System.err.println("Error starting file server on port " + port + ": " + e.getMessage());
        }
    }

    // Inner class that handles the actual file sending to a connected client
    private static class FileSenderHandler implements Runnable {
        // Store the client's socket connection
        private final Socket clientSocket;
        // Store the path to the file that needs to be sent
        private final String filePath;

        // Constructor for FileSenderHandler
        public FileSenderHandler(Socket clientSocket, String filePath) {
            // Store the client socket for communication
            this.clientSocket = clientSocket;
            // Store the file path to be sent
            this.filePath = filePath;
        }

        // This method is called when the thread starts running
        @Override
        public void run() {
            // Try to open the file and get the output stream to the client
            try (FileInputStream fis = new FileInputStream(filePath);
                 OutputStream oss = clientSocket.getOutputStream()) {
                
                // Send the filename as a header before the file content
                // This helps the client know what to name the downloaded file

                String filename = new File(filePath).getName();/* creates a new file object, filePath is a String 
                variable that contains the path to a file (for example, "C:/Users/John/Documents/report.pdf").
                
                For example, if the path is "C:/Users/John/Documents/report.pdf", getName() will return "report.pdf".*/
                
                // Create a header string with the filename
                String header = "Filename: " + filename + "\n";
                // Write the header to the client
                oss.write(header.getBytes());
                
                // Send the actual file content in chunks
                // Create a buffer to read file data in chunks of 4096 bytes
                byte[] buffer = new byte[4096];
                // Variable to store how many bytes were read in each iteration
                int bytesRead;
                // Read the file in chunks and send each chunk to the client
                while ((bytesRead = fis.read(buffer)) != -1) {
                    // Write the chunk of data to the client
                    oss.write(buffer, 0, bytesRead);
                }
                // Print a success message when the file has been sent
                System.out.println("File '" + filename + "' sent to " + clientSocket.getInetAddress());
            } catch (IOException e) {
                // Print an error message if there's a problem sending the file
                System.err.println("Error sending file to client: " + e.getMessage());
            } finally {
                // Always try to close the client socket, even if an error occurred
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Print an error if there's a problem closing the socket
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }

}
