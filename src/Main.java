import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;


import java.nio.file.Path;

class TCPServer {
    public static void main(String argv[]) throws Exception {
        String clientSentence;
        String capitalizedSentence;
        int socketPort = Integer.parseInt(argv[0]);
        String addressName = argv[1];

        //Displays port number and address
        System.out.println(argv[0]);
        System.out.println(argv[1]);
        System.out.println();

        ServerSocket welcomeSocket = new ServerSocket(socketPort);
        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            // Read request from client and parse it
            String requestString = "";
            String line;
            while ((line = inFromClient.readLine()) != null && !line.isEmpty()) {
                requestString += line + "\r\n";
            }
            HTTPRequest httpRequest = new HTTPRequest(requestString, addressName);

            //Check for port and address.
            if (argv.length < 2 || argv[0].isEmpty() || argv[1].isEmpty()) {
                // Return a 400 Bad Request error for missing arguments
                String response = "HTTP/1.1 400 Bad Request\r\n\r\n";
                outToClient.writeBytes(response);
            }

            //Get the path and add index.html to concatenate to end of root
            String path = httpRequest.getPath();
            if (path.endsWith("/")) {
                path += "index.html";
            }
            String rootDirectory = "\\C:\\Users\\hunter\\IdeaProjects\\proj1\\";
            File file = new File(rootDirectory + path);

            System.out.println("Modified since: " + httpRequest.getIfModifiedSince());
            System.out.println("Path: " + file);
            System.out.println("Command: " + httpRequest.getMethod());
            System.out.println();


            // Handle the request based on its method
            if (httpRequest.getMethod().equals("GET")) {
                if (!file.exists()) {
                    // Handle 404 Not Found error
                    String response = "HTTP/1.1 404 Not Found\r\n\r\n";
                    outToClient.writeBytes(response);
                } else if (file.isDirectory()) {
                    // Handle directory listing or redirect to index.html
                    // TODO: Implement directory listing or redirect logic
                } else {
                    // Serve the requested file
                    String contentType = getContentType(file);
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    String response = "HTTP/1.1 200 OK\r\n";
                    response += "Date: " + getCurrentDate() + "\r\n";
                    response += "Server: MyServer\r\n";
                    response += "Last-Modified: " + getLastModified(file) + "\r\n";
                    response += "Content-Type: " + contentType + "\r\n";
                    response += "Content-Length: " + fileContent.length + "\r\n\r\n";

                    outToClient.writeBytes(response);
                    System.out.println("Sending response:\n" + response);
                    outToClient.write(fileContent, 0, fileContent.length);
                }

            } else if (httpRequest.getMethod().equals("HEAD")) {
                if (!file.exists()) {
                    // Handle 404 Not Found error
                } else {
                    // Serve only the header
                }
            } else {
                // Return a 501 Not Implemented error for unsupported methods
                String response = "HTTP/1.1 501 Not Implemented\r\n\r\n";
                outToClient.writeBytes(response);
            }

            connectionSocket.close();
        }
    }
    private static String getLastModified(File file) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(file.lastModified());
    }



    private static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date());
    }


    private static String getContentType(File file) {
        String fileName = file.getName();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        switch (extension.toLowerCase()) {
            case "html":
                return "text/html";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

}



