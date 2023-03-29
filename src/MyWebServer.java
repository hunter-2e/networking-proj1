import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.text.ParseException;

class TCPServer {
    public static void main(String argv[]) throws Exception {
        int socketPort = Integer.parseInt(argv[0]);
        String addressName = argv[1];

        //Open connection to server
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

            //Makes HTTPRequest to be parsed
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

            String rootDirectory = addressName;

            File file = new File(rootDirectory + path);

            //Processes GET request
            if (httpRequest.getMethod().equals("GET")) {
                if (!file.exists()) {
                    // Handle 404 Not Found error
                    String response = "HTTP/1.1 404 Not Found\r\n\r\n";
                    outToClient.writeBytes(response);
                } else {
                    // Check If-Modified-Since header and compare with file's last modified date
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
                    dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
                    Date ifModifiedSince = null;
                    String ifModifiedSinceStr = httpRequest.getHeader("If-Modified-Since");
                    if (ifModifiedSinceStr != null) {
                        try {
                            ifModifiedSince = dateFormat.parse(ifModifiedSinceStr);
                        } catch (ParseException e) {
                            String response = "HTTP/1.1 400 bad request\r\n\r\n";
                            outToClient.writeBytes(response);
                            continue;
                        }
                    }

                    long lastModified = file.lastModified();
                    if (ifModifiedSince != null && ifModifiedSince.getTime() >= lastModified) {
                        // Return 304 Not Modified response
                        String response = "HTTP/1.1 304 Not Modified\r\n";
                        response += "Date: " + getCurrentDate() + "\r\n";

                        outToClient.writeBytes(response);
                        inFromClient.close();
                        outToClient.close();
                        System.out.println("Sending response:\n" + response);
                    } else {
                        // Serve the requested file
                        byte[] fileContent = Files.readAllBytes(file.toPath());
                        String response = "HTTP/1.1 200 OK\r\n";
                        response += "Date: " + getCurrentDate() + "\r\n";
                        response += "Server: Hunter's Server\r\n";
                        response += "Last-Modified: " + getLastModified(file) + "\r\n";
                        response += "Content-Length: " + fileContent.length + "\r\n\r\n";

                        outToClient.writeBytes(response);
                        System.out.println("Sending response:\n" + response);
                        outToClient.write(fileContent, 0, fileContent.length);
                    }
                }
                //Processes HEAD request
            } else if (httpRequest.getMethod().equals("HEAD")) {
                if (!file.exists()) {
                    // Handle 404 Not Found error
                    String response = "HTTP/1.1 404 Not Found\r\n\r\n";
                    outToClient.writeBytes(response);
                } else {
                    // Check If-Modified-Since header and compare with file's last modified date
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
                    dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
                    Date ifModifiedSince = null;
                    String ifModifiedSinceStr = httpRequest.getHeader("If-Modified-Since");
                    if (ifModifiedSinceStr != null) {
                        try {
                            ifModifiedSince = dateFormat.parse(ifModifiedSinceStr);
                        } catch (ParseException e) {
                            String response = "HTTP/1.1 400 bad request\r\n\r\n";
                            outToClient.writeBytes(response);
                            continue;
                        }
                    }
                    long lastModified = file.lastModified();
                    if (ifModifiedSince != null && ifModifiedSince.getTime() >= lastModified) {
                        // Return 304 Not Modified response
                        String response = "HTTP/1.1 304 Not Modified\r\n";
                        response += "Date: " + getCurrentDate() + "\r\n";

                        outToClient.writeBytes(response);
                        inFromClient.close();
                        outToClient.close();
                        System.out.println("Sending response:\n" + response);
                    } else {
                        // Serve the requested file
                        byte[] fileContent = Files.readAllBytes(file.toPath());
                        String response = "HTTP/1.1 200 OK\r\n";
                        response += "Date: " + getCurrentDate() + "\r\n";
                        response += "Server: Hunter's Server\r\n";
                        response += "Last-Modified: " + getLastModified(file) + "\r\n";
                        response += "Content-Length: " + fileContent.length + "\r\n\r\n";

                        outToClient.writeBytes(response);
                        System.out.println("Sending response:\n" + response);

                        inFromClient.close();
                        outToClient.close();
                        connectionSocket.close();
                    }
                }
            }
            //Processes anything other than HEAD and GET requests
            else {
                // Return a 501 Not Implemented error for unsupported methods
                String response = "HTTP/1.1 501 Not Implemented\r\n\r\n";
                outToClient.writeBytes(response);
                inFromClient.close();
                outToClient.close();
            }
        }
    }
    //Gets and formats last modified date
    private static String getLastModified(File file) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
        return dateFormat.format(file.lastModified());
    }
    //Gets and formats current date
    private static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
        return dateFormat.format(new Date());
    }

    //HTTPRequest class headers
    public static class HTTPRequest {
        private String method;
        private String path;

        private String[] lines;
        public HTTPRequest(String request, String rootPath) {
            lines = request.split("\\r?\\n");
            String[] requestLineTokens = lines[0].split(" ");
            method = requestLineTokens[0];
            path = rootPath + requestLineTokens[1];
        }
        //Checks if header is present and parses it properly ("If-Modified-Since")
        public String getHeader(String headerName) {
            String headerValue = null;
            for (int i = 1; i < lines.length; i++) {
                String[] headerTokens = lines[i].split(": ");
                if (headerTokens[0].equals(headerName)) {
                    headerValue = headerTokens[1];
                    break;
                }
            }
            return headerValue;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }
    }
}



