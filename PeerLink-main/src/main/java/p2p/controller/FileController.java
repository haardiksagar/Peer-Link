package p2p.controller;

import p2p.service.FileSharer;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.io.IOUtils;

public class FileController {
    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(10);
        
        File uploadDirFile = new File(uploadDir);/*new File(uploadDir): This creates a File object that 
        points to the directory specified by uploadDir. It does not create the directory on disk yet; it 
        just creates an object representing that path.*/
        if (!uploadDirFile.exists()) { //uploadDirFile =  objec that points to the "peerlink-uploads file"
            uploadDirFile.mkdirs();/*This method creates the directory represented by uploadDirFile, 
            For example, if peerlink-uploads or any parent folder does not exist, it will create them all.*/
        
            /*
            Example
            Suppose uploadDir is "C:\\Users\\John\\AppData\\Local\\Temp\\peerlink-uploads":
            If "peerlink-uploads" does not exist, this code will create it.
            If it already exists, nothing happens and the code moves on.
            */
        }
        
        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());
        
        server.setExecutor(executorService);
    }
    
    public void start() {
        server.start();
        System.out.println("API server started on port " + server.getAddress().getPort());
    }
    
    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("API server stopped");
    }
    
    private class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            /*HttpExchange exchange: This object represents the HTTP request and response. It contains: 

            Here's a small EXAMPLE showing what data is available in an HttpExchange object:
            -----------------------
            Method: POST
            Path: /api/users
            Headers: {
                Authorization=[Bearer abc123token], 
                Content-type=[application/json], Host=[localhost:8080]  ← This tells us what's in the body
                }
            Request Body: {"name": "Alice", "email": "alice@example.com"}  ← This IS the JSON data
            ---------------------------
            The HttpExchange object is your gateway to everything about the HTTP request coming in and
            provides the tools to send a response back to the client.

            HttpExchange with Multipart Data:

            Method: POST
            Path: /api/profiles
            Headers: {Authorization=[Bearer abc123token], Content-type=[multipart/form-data; boundary=----WebKitFormBoundary1234], Host=[localhost:8080], Content-length=[1547]}
            Request Body: ------WebKitFormBoundary1234
            Content-Disposition: form-data; name="username"

            Alice
            ------WebKitFormBoundary1234
            Content-Disposition: form-data; name="email"

            alice@example.com
            ------WebKitFormBoundary1234
            Content-Disposition: form-data; name="photo"; filename="selfie.jpg"
            Content-Type: image/jpeg

            ����JFIF��������C

            ��C												��  � �"��
            ------WebKitFormBoundary1234--
            */
            Headers headers = exchange.getResponseHeaders();
            /* difference between getResponseHeaders() and getRequestHeaders()
            getResponseHeaders() - What YOU Send to CLIENT
            Headers responseHeaders = exchange.getResponseHeaders();

            What it contains: Headers that your server will send back to the client
            Example of what you put in:

            Content-Type: application/json
            Set-Cookie: sessionId=xyz789
            Access-Control-Allow-Origin: *
            Cache-Control: no-cache
            
            Think of it as: Writing on the envelope that you're sending back
            */
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            String response = "Not Found";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    private static class MultipartParser {
        private final byte[] data;
        private final String boundary;
        
        public MultipartParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }
        
        public ParseResult parse() {
            try {
                String dataAsString = new String(data);
                
                String filenameMarker = "filename=\"";
                int filenameStart = dataAsString.indexOf(filenameMarker);
                if (filenameStart == -1) {
                    return null;
                }
                
                filenameStart += filenameMarker.length();
                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String filename = dataAsString.substring(filenameStart, filenameEnd);
                
                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
                String contentType = "application/octet-stream"; // Default
                
                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
                }
                
                String headerEndMarker = "\r\n\r\n";
                int headerEnd = dataAsString.indexOf(headerEndMarker);
                if (headerEnd == -1) {
                    return null;
                }
                
                int contentStart = headerEnd + headerEndMarker.length();
                
                byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();
                int contentEnd = findSequence(data, boundaryBytes, contentStart);
                
                if (contentEnd == -1) {
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    contentEnd = findSequence(data, boundaryBytes, contentStart);
                }
                
                if (contentEnd == -1 || contentEnd <= contentStart) {
                    return null;
                }
                
                byte[] fileContent = new byte[contentEnd - contentStart];
                System.arraycopy(data, contentStart, fileContent, 0, fileContent.length);
                
                return new ParseResult(filename, contentType, fileContent);
            } catch (Exception e) {
                System.err.println("Error parsing multipart data: " + e.getMessage());
                return null;
            }
        }
        
        private int findSequence(byte[] data, byte[] sequence, int startPos) {
            outer:
            for (int i = startPos; i <= data.length - sequence.length; i++) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }
        
        public static class ParseResult {
            public final String filename;
            public final String contentType;
            public final byte[] fileContent;
            
            public ParseResult(String filename, String contentType, byte[] fileContent) {
                this.filename = filename;
                this.contentType = contentType;
                this.fileContent = fileContent;
            }
        }
    }
    /*
    * UploadHandler is a class you wrote that implements the HttpHandler interface.
    * This means it must provide the handle method.
    * When someone sends a request to /upload, the server calls handle() in your UploadHandler to process that request.
    */
    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            /*
             *Here's an example of HTTP Exchange objects header section in a request:(Header is a section in whole HTTP Exchange object)
             
             This is a GET request header 
                GET /api/users HTTP/1.1
                Host: example.com
                User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
                Accept: application/json
                Content-Type: application/json
                Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                Cache-Control: no-cache
                Connection: keep-alive
            */

            //Headers: A collection object that stores multiple header key-value pairs
            Headers headers = exchange.getResponseHeaders();
            //(CORS) "*"->"Any website from anywhere can access my data"
            headers.add("Access-Control-Allow-Origin", "*");/*The "*" is like a wild card that means "everyone is welcome!"
            You could also be more specific:
            
            "https://friendsite.com" = "Only my friend's site can visit"
            "*" = "Everyone can visit!"
            
            So this one line of code is basically your server being friendly and welcoming to visitors from other websites, 
            instead of being unfriendly and saying "go away!"
            ------------------------------------------------------------
            Let's say you're on Google Search (https://www.google.com) and you click on Google Maps:
            What happens:

            You're currently on origin: https://www.google.com
            Maps is at origin: https://maps.google.com
            These are different origins (different subdomains)
            If Maps wants to share data with Search, it needs CORS permission!

            How Google Handles This
            Google's servers probably have headers like:
            Access-Control-Allow-Origin: https://www.google.com
            ------------------------------------------------------------
            *Only allow the bank's own mobile app website
            headers.add("Access-Control-Allow-Origin", "https://mobile.mybank.com");
            ------------------------------------------------------------
            This doesn't work:
            headers.add("Access-Control-Allow-Origin", "https://site1.com, https://site2.com");
            ------------------------------------------------------------
            Option 1: Allow EVERYONE (not recommended for sensitive data)
            headers.add("Access-Control-Allow-Origin", "*");//* = everyone can access my data
            This means: "Any website from anywhere can access my data"

            Option 2: Allow SPECIFIC websites only
            headers.add("Access-Control-Allow-Origin", "https://www.google.com");
            This means: "Only Google Search can access my data"
            */
            /*
             What does this mean?-> headers.add("Access-Control-Allow-Origin", "*")
                If someone knows the address (URL and port) of your server, any website can send requests to upload or download files.
                This is because your server is telling browsers: “I accept requests from anywhere.”
            */
             // Only process POST requests for uploads
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                //if(exchange.getRequestMethod() != "POST") then perform the inner bracket
                /* 
                1. exchange.getRequestMethod(): Gets the HTTP method (GET, POST, PUT, DELETE, etc.)
                2. .equalsIgnoreCase("POST"): Checks if the method is "POST" (case-insensitive)
                3. !: NOT operator - so this checks if it's NOT a POST request
                4. return: Exits the method early if it's not a POST request 
                */
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);//explaination below
                /*
                Purpose: Sends the HTTP status code and headers to the client
                Parameter 1 (405): HTTP status code meaning "Method Not Allowed"
                Parameter 2 (response.getBytes().length): The exact size of the response body in byt
                response.getBytes()

                What it does: Converts the string "Method Not Allowed" into an array of bytes
                Why needed: HTTP sends data as bytes, not as text
                Example: "Hi" becomes [72, 105] (ASCII values)

                *Example: "Method Not Allowed" = 17 bytes
                */
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                /*
                 OutputStream os = exchange.getResponseBody():
                1.exchange.getResponseBody(): Gets the output stream where you write the response
                2. OutputStream: A class for writing bytes to a destination
                3. os: Variable name (short for "output stream")
                4. Think of it as: A pipe where you can pour data that goes to the client

                    5.os.write(response.getBytes()):->
                    *What it does: Writes the byte array to the output stream
                    *response.getBytes(): Converts "Method Not Allowed" to bytes again
                    *Flow: String → Bytes → Network → Client's browser
                ----------------------------------------------------------
                /*HTTP Status Code 405
                What it means: "I understand your request, but I don't support that HTTP method"

                Common scenario:
                Client sends POST request
                Your server only handles GET requests
                You return 405 to say "try GET instead"
                */ 
                return;
                //HOW THIS CODE WORKS IN REAL WORLD EXAMPLE of the above code please check it on claude notes as well:
                /*
                Real-World Example Context
                This code typically appears in handlers like this:

                java@Override
                public void handle(HttpExchange exchange) throws IOException {
                    String method = exchange.getRequestMethod();

                    if (method.equals("GET")) {
                        // Handle GET request
                        String response = "Hello World";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                    } else {
                        // Your code goes here - reject other methods
                        String response = "Method Not Allowed";
                        exchange.sendResponseHeaders(405, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                    }
                }
                */
            }
            
            Headers requestHeaders = exchange.getRequestHeaders();
            /*
            getRequestHeaders() - What CLIENT Sent to YOU
            Headers requestHeaders = exchange.getRequestHeaders();

            What it contains: Headers that the client (browser/app) sent to your server
            Example of what's inside:

            User-Agent: Mozilla/5.0 (Chrome)
            Accept: application/json
            Content-Type: multipart/form-data
            Authorization: Bearer abc123
            Host: myserver.com

            Think of it as: Reading the envelope that someone sent to your mailbox
            */
            String contentType = requestHeaders.getFirst("Content-Type");
            /*
            Example 3: Multiple Content-Type (Rare but possible)

            Header:
                POST /upload HTTP/1.1
                Host: example.com
                Content-Type: multipart/form-data; boundary=----WebKitFormBoundary
                Content-Type: text/plain

            Codes Result:(getFirst()-> it gets the very first thing of the kind mentioned in the braces
            in the case above we want the first "Content-Type" so we go multipart/ etc)
                
                String firstContentType = requestHeaders.getFirst("Content-Type");
                Result: "multipart/form-data; boundary=----WebKitFormBoundary"
            */

            //This "if" cond: // Check if the request is correctly formatted (multipart/form-data)
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                /*
                 WHAT IS "multipart/form-data"?
                    Think of multipart/form-data like a package with multiple compartments:
                    Regular Form Data (application/x-www-form-urlencoded)
                    name=John&age=25&email=john@email.com

                    Like a single envelope with text written on it
                    Good for simple text fields only
                --------------------------------------------------
                
                application/json Example

                {
                  "name": "John",
                  "age": 25,
                  "email": "john@email.com"
                }

                multipart/form-data Example

                ------WebKitFormBoundary1234
                Content-Disposition: form-data; name="name"

                John
                ------WebKitFormBoundary1234
                Content-Disposition: form-data; name="photo"; filename="pic.jpg"
                Content-Type: image/jpeg

                [BINARY IMAGE DATA]
                ------WebKitFormBoundary1234--

            ---------------------------------------------------------
                Breaking Down multipart/form-data:

                    The Boundary Line
                    ------WebKitFormBoundary1234
                    What it is: A separator line that divides different parts
                    Think of it as: Walls between different sections of a package
                    First Part (Text Field)
                    Content-Disposition: form-data; name="name"

                    John

                    Content-Disposition: Says "this is form data"
                    name="name": This data belongs to a form field called "name"
                    Empty line: Separates headers from actual data
                    John: The actual value of the "name" field

                    Second Part (File Upload)
                    Content-Disposition: form-data; name="photo"; filename="pic.jpg"
                    Content-Type: image/jpeg

                    [BINARY IMAGE DATA]

                    name="photo": This belongs to a form field called "photo"
                    filename="pic.jpg": The original file name
                    Content-Type: image/jpeg: Says "this is a JPEG image"
                    [BINARY IMAGE DATA]: The actual file bytes (not readable text)

                    End Boundary
                    ------WebKitFormBoundary1234--
                    The double dashes -- at the end mean "this is the final boundary, no more parts coming"
                    Simple Analogy
                    Think of it like a mail package with labeled compartments:

                    Boundary lines = cardboard dividers
                    First compartment = a note saying "My name is John"
                    Second compartment = a photo labeled "pic.jpg"
                    Final boundary = "package ends here"

                    HTML Form That Creates This
                    html<form method="POST" enctype="multipart/form-data">
                        <input name="name" value="John">           <!-- Creates first part -->
                        <input name="photo" type="file">           <!-- Creates second part -->
                    </form>
                    When submitted, this form generates exactly that multipart data!
                */
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            try {
                // 1. Parse the incoming file data from the request as its multipart/form-data
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(exchange.getRequestBody(), baos);
                byte[] requestData = baos.toByteArray();//this is a byte[] (byte array) containing the raw data 
                //from the HTTP request body and not just the file bytes
                //Looks like this in he requestData
                /*byte[] requestData = {
                    45, 45, 45, 45, 45, 45, 87, 101, 98, 75, 105, 116, // "------WebKit"
                    65, 108, 105, 99, 101,                              // "Alice"
                    255, 216, 255, 224, 0, 16, 74, 70, 73, 70,        // JPEG file bytes
                    // ... thousands more bytes
                };
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();->
                    *Creates an in-memory buffer that grows dynamically as you write data to it
                    *Think of it like a resizable container that stores bytes

                exchange.getRequestBody():->
                    *Returns an InputStream representing the incoming HTTP request body
                    *This could be JSON data, form data, file uploads, etc.

                IOUtils.copy(exchange.getRequestBody(), baos);->
                    *Reads all bytes from the request body stream and writes them to the ByteArrayOutputStream
                    *This is a utility method that handles the reading loop for you

                baos.toByteArray()->
                    *Converts the accumulated bytes into a standard byte array
                    *Now you have the complete request body in memory
                
                */ 

                
                MultipartParser parser = new MultipartParser(requestData, boundary);
                MultipartParser.ParseResult result = parser.parse();
                /*
                Explaining what is "Parsing" Like You're a Little Kid 

                What is parsing? Parsing is like taking apart a LEGO castle to see all the individual pieces!
                Imagine you have a big box of mixed-up LEGO pieces all jumbled together. Parsing is when you:

                1.Separate all the pieces
                2.Sort them by color and type
                3.Organize them so you can use each piece properly

                Example for a kid: Let's say you have a sentence written without spaces: "catdogbirdfish"
                ->Parsing this would mean breaking it apart into separate words:
                "cat" + "dog" + "bird" + "fish"
                Now you can understand each word individually! 

                ->Real-World Example: File Upload Parsing
                The Problem: When you upload a file on a website (like uploading a photo to Facebook), 
                the computer receives everything mixed together in one big chunk, like this messy package:

                    ------BOUNDARY123
                    Content-Disposition: form-data; name="username"

                    john_doe
                    ------BOUNDARY123
                    Content-Disposition: form-data; name="photo"; filename="vacation.jpg"
                    Content-Type: image/jpeg

                    [binary photo data here - lots of computer gibberish]
                    ------BOUNDARY123
                    Content-Disposition: form-data; name="caption"

                    My awesome vacation photo!
                    ------BOUNDARY123--

                What Parsing Does: The MultipartParser takes this messy chunk and separates it into organized pieces:

                1.Username field: john_doe
                2.Photo file:
                    *Name: vacation.jpg
                    *Type: image/jpeg
                    *Content: [the actual photo data]
                3.Caption field: My awesome vacation photo!

                Why This Matters: Now the server can:
                    Save the photo with the correct filename (vacation.jpg)
                    Know it's a JPEG image
                    Store the username (john_doe)
                    Display the caption (My awesome vacation photo!)

                Without parsing, the server would just see one big confusing mess and wouldn't know 
                where the photo starts, where the username is, or what the filename should be!
                */
                
                if (result == null) {
                    String response = "Bad Request: Could not parse file content";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }
                
                String filename = result.filename;
                if (filename == null || filename.trim().isEmpty()) {
                    filename = "unnamed-file";
                }
                
                /*will tmp file in C drive stores multiple users files when uploaded at the same time?
                 ANS: System.getProperty("java.io.tmpdir") usually points to something like C:\Users\<username>\AppData\Local\Temp.
                 
                This means even if two users upload files with the same name at the same time, their files will have different names in the temp folder because of the random UUID prefix.
                Yes, the temp folder will store multiple users' files at the same time.
                Each file will have a unique name, so they won’t overwrite each other.
                All files are stored in the same directory (peerlink-uploads inside the system temp directory), but their names are unique.

                Suppose two users upload a file called photo.jpg at the same time:
                User 1’s file might be saved as:
                C:\Users\<username>\AppData\Local\Temp\peerlink-uploads\c1a2b3c4-5678-1234-9abc-def012345678_photo.jpg
                User 2’s file might be saved as:
                C:\Users\<username>\AppData\Local\Temp\peerlink-uploads\9f8e7d6c-5432-4321-8fed-cba987654321_photo.jpg
                */
                String uniqueFilename = UUID.randomUUID().toString() + "_" + new File(filename).getName();
                String filePath = uploadDir + File.separator + uniqueFilename;
                
                // 2. Save the file content to the temporary upload directory
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(result.fileContent);
                }
                
                int port = fileSharer.offerFile(filePath);
                
                new Thread(() -> fileSharer.startFileServer(port)).start();
                
                String jsonResponse = "{\"port\": " + port + "}";//explained below
                /*
                * JSON: JavaScript Object Notation - a data format like {"port": 8081}
                * \"port\": Escaped quotes to include quotes in the string
                * Purpose: Tells the frontend which port to use for downloading
                */
                headers.add("Content-Type", "application/json");
                /*
                Set Response Headers:->
                    exchange.getResponseHeaders().add("Content-Type", "application/json");

                    Content-Type: Header that tells the client what type of data is being sent
                    application/json: MIME type indicating JSON data

                    Send Response:->
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

                    sendResponseHeaders(): Sends the HTTP status code and headers
                    200: HTTP status code for "OK" (success)
                    jsonResponse.getBytes().length: The size of the response body in bytes
                */
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                /*this above statement is sending the JSON data to the fronted end that will be 
                transformed into a invite code for the uploader to send to the one who is going to dowload the file*/
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
                
            } catch (Exception e) {
                System.err.println("Error processing file upload: " + e.getMessage());
                String response = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
    
    // Handler class for file downloads
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Get the response headers object to set HTTP headers
            Headers headers = exchange.getResponseHeaders();
            // Allow requests from any origin (CORS)
            headers.add("Access-Control-Allow-Origin", "*");
            
            // Only allow GET requests for downloads
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                // If the request is not GET, respond with 405 Method Not Allowed
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            // Get the request path (should contain the port number)
            String path = exchange.getRequestURI().getPath();
            // Extract the port number from the path (after the last '/')
            String portStr = path.substring(path.lastIndexOf('/') + 1);
            
            try {
                // Convert the port string to an integer
                int port = Integer.parseInt(portStr);
                
                // Connect to the file server running on the given port
                try (Socket socket = new Socket("localhost", port);
                     InputStream socketInput = socket.getInputStream()) {
                    
                    // Create a temporary file to store the downloaded data
                    File tempFile = File.createTempFile("download-", ".tmp");
                    // Default filename in case it's not provided by the server
                    String filename = "downloaded-file"; // Default filename
                    
                    // Write the data from the socket to the temp file
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        // Buffer to read file data in chunks
                        byte[] buffer = new byte[4096];
                        // Variable to store how many bytes were read in each iteration
                        int bytesRead;
                        
                        // Read the header line from the socket (contains filename)
                        ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
                        int b;
                        // Read bytes one by one until a newline is found (end of header)
                        while ((b = socketInput.read()) != -1) {
                            if (b == '\n') break;
                            headerBaos.write(b);
                        }
                        
                        // Parse the filename from the header if present
                        String header = headerBaos.toString().trim();
                        if (header.startsWith("Filename: ")) {
                            filename = header.substring("Filename: ".length());
                        }
                        
                        // Read the rest of the file data and write to disk
                        while ((bytesRead = socketInput.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // Set the response headers to prompt a file download in the browser
                    headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                    headers.add("Content-Type", "application/octet-stream");
                    
                    // Send a 200 OK response with the file size
                    exchange.sendResponseHeaders(200, tempFile.length());
                    // Write the file data to the response body
                    try (OutputStream os = exchange.getResponseBody();
                         FileInputStream fis = new FileInputStream(tempFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        // Read the file in chunks and send each chunk to the client
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // Delete the temporary file after sending
                    tempFile.delete();
                    
                } catch (IOException e) {
                    // Print an error if something goes wrong with the socket
                    System.err.println("Error downloading file from peer: " + e.getMessage());
                    // Respond with 500 Server Error
                    String response = "Error downloading file: " + e.getMessage();
                    headers.add("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(500, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
                
            } catch (NumberFormatException e) {
                // Respond with 400 Bad Request if the port is invalid
                String response = "Bad Request: Invalid port number";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
