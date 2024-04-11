import java.io.*;
import java.net.*;
import java.util.HashMap;

class EchoRunnable implements Runnable {
    private void createParamsHtml(HTTPRequest req) {
        String filePath = this.root + "params_info.html";
        HashMap<String, String> params = req.getParameters();
        // Variables with their values
        String feedback = params.get("feedback");
        String subscribe = (params.get("subscribe") != null && params.get("subscribe").equals("yes")) ? "yes" : "no";
        // Building the HTML content
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Parameter Details</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h2>Submitted Parameters</h2>\n" +
                "    <p>feedback: " + feedback + "</p>\n" +
                "    <p>subscribe: " + subscribe + "</p>\n" +
                "</body>\n" +
                "</html>";

        // Write the HTML content to a file
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(htmlContent);
            bw.close();
            System.out.println("HTML content has been written to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleRequest(String requestLine, String requestBody, DataOutputStream outToClient) throws IOException {
        HTTPRequest req = new HTTPRequest(requestLine, requestBody);
        if (req.getBadRequest()) {
            outToClient.write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
            return;
        }
	System.out.println("Request headers:");
        System.out.println(requestLine);
	System.out.println("Request body:");
        System.out.println(requestBody);
        if (req.getType().equals("GET") || req.getType().equals("POST") || req.getType().equals("HEAD")) {
            handleGETPOSTHEAD(req, outToClient);
        }  else if (req.getType().equals("TRACE")) {
            handleTRACE(req, outToClient, requestLine);
        } else {
            outToClient.write("HTTP/1.1 501 Not Implemented\r\n\r\n".getBytes());
        }
    }
    private String getContentType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".ico")) {
            return "icon";
        } else {
            return "application/octet-stream";
        }
    }
    private void handleGETPOSTHEAD(HTTPRequest req, DataOutputStream outToClient) {
        String requestedPage = req.getRequestedPage();
        requestedPage = requestedPage.replaceAll("../", "");
        if (requestedPage.equals("/")) {
            requestedPage = defaultPage;
        }
        if (requestedPage.equals("/params_info.html")){
            createParamsHtml(req);
        }
        requestedPage = this.root + requestedPage.substring(1);
        File file = new File(requestedPage);
        if (file.exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                String contentType = getContentType(requestedPage);
                String responseString = "HTTP/1.1 200 OK\r\n" + "Content-Type: " + contentType + "\r\n";
                outToClient.write(responseString.getBytes());
                System.out.println("Response:");
                System.out.println(responseString);
                if (req.getChunked()) {
                    // Start sending chunked data
                    outToClient.write("Transfer-Encoding: chunked\r\n\r\n".getBytes());
                    System.out.println("Transfer-Encoding: chunked\r\n\r\n".getBytes());
                    if (!req.getType().equals("HEAD")) {
                        final int CHUNK_SIZE = 1024; // Define chunk size
                        byte[] buffer = new byte[CHUNK_SIZE];
                        int bytesRead;
                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            String hexLength = Integer.toHexString(bytesRead) + "\r\n";
                            outToClient.write(hexLength.getBytes());
                            outToClient.write(buffer, 0, bytesRead);
                            outToClient.write("\r\n".getBytes());
                        }
                        outToClient.write("0\r\n\r\n".getBytes()); // Signal end of chunked data
                    }
                } else {
                    // For non-chunked responses
                    long contentLength = file.length();
                    byte[] fileContent = new byte[(int) contentLength];
                    int bytesRead = fileInputStream.read(fileContent);
                    outToClient.write(("Content-Length: " + contentLength + "\r\n\r\n").getBytes());
                    System.out.println("Content-Length: " + contentLength + "\r\n\r\n");
                    if (!req.getType().equals("HEAD")) {
                        outToClient.write(fileContent);
                    }
                }
            } catch (IOException e) {
                try {
                    outToClient.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
                } catch (IOException ioException) {
                    // Handle potential IOException from writing the error response
                }
            }
        } else {
            try {
                outToClient.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            } catch (IOException e) {
                // Handle potential IOException from writing the 404 response
            }
        }
    }
    
    
    private void handleTRACE(HTTPRequest req, DataOutputStream outToClient, String reqLine) {
        try {
            String requestHeaders = "HTTP/1.1 200 OK\r\nContent-Type: message/http\r\n\r\n" + req.toString();
            outToClient.write(requestHeaders.getBytes());
            outToClient.write(reqLine.getBytes());
        } catch (IOException e) {
            try {
                outToClient.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private Socket clientSocket = null;
    private String root;
    private String defaultPage;

    EchoRunnable(Socket clientSocket, String root, String defaultPage) {
        this.clientSocket = clientSocket;
        this.root = root;
        this.defaultPage = defaultPage;
//        try {
//            this.clientSocket.setSoTimeout(500);
//        } catch (SocketException e) {
//        }

    }

    @Override
    public void run() {
        StringBuilder requestHeaders = new StringBuilder();
        try (
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            String clientSentence;
            // Keep reading lines until a blank line is reached, indicating the end of the request headers
            while ((clientSentence = inFromClient.readLine()) != null && !clientSentence.isEmpty()) {
                requestHeaders.append(clientSentence).append("\r\n");
            }
            
            int contentLength = 0;
            for (String header : requestHeaders.toString().split("\n")) {
                if (header.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(header.substring("content-length:".length()).trim());
                    } catch (NumberFormatException e) {
                        // Handle malformed Content-Length header
                    }
                    break;
                }
            }
            
            String requestBody = "";
            if (contentLength > 0) {
                char[] body = new char[contentLength];
                int bytesRead = inFromClient.read(body, 0, contentLength);
                if (bytesRead != contentLength) {
                    // Handle case where actual bytesRead doesn't match Content-Length header
                }
                requestBody = new String(body, 0, bytesRead);
            }
            if (requestHeaders.length() > 0) {
                handleRequest(requestHeaders.toString(), requestBody, outToClient);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                this.clientSocket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }
    
}
