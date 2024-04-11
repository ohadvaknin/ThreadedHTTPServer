import java.net.URLDecoder;
import java.util.HashMap;

public class HTTPRequest {
    private String type;
    private String requestedPage;
    private boolean isImage;
    private int contentLength = 0; // Default to 0
    private String referer;
    private String userAgent;
    private boolean badRequest;
    private boolean chunked;
    private HashMap<String, String> parameters = new HashMap<>();

    private boolean isBadRequest(String requestHeader, String requestBody) {
        String[] lines = requestHeader.split("\r\n");
        if (lines.length == 0) return true; // Empty request header
    
        // Check request line validity
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length != 3) return true; // Malformed request line
    
        // Supported methods
        String method = requestLine[0];
        if (!(method.equals("GET") || method.equals("POST") || method.equals("HEAD") || method.equals("TRACE"))) {
            return true; // Unsupported method
        }
    
        // Basic URI validation
        String uri = requestLine[1];
        if (uri.contains("..") || !uri.startsWith("/")) {
            return true; // Potential directory traversal or malformed URI
        }
    
        // Check for valid Content-Length if method is POST
        if (method.equals("POST")) {
            boolean contentLengthFound = false;
            for (String line : lines) {
                if (line.startsWith("Content-Length: ")) {
                    try {
                        int contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                        if (contentLength != requestBody.length()) return true; // Content-Length mismatch
                        contentLengthFound = true;
                    } catch (NumberFormatException e) {
                        return true; // Invalid Content-Length value
                    }
                }
            }
            if (!contentLengthFound) return true; // Missing Content-Length header for POST request
        }
    
        // All checks passed
        return false;
    }
    
    public HTTPRequest(String requestHeader, String requestBody) {
        // Parse the request header
        this.badRequest = isBadRequest(requestHeader, requestBody);
        if (this.badRequest) return;
        String[] lines = requestHeader.split("\r\n");
        // Extract type and requested page
        String[] requestLine = lines[0].split(" ");
        this.type = requestLine[0];
        String requestedPageWithParams = requestLine[1];
        // Check for fragment identifier
        int fragmentIndex = requestedPageWithParams.indexOf('#');
        if (fragmentIndex != -1) {
            // If there is a fragment, simply ignore it for server-side processing
            requestedPageWithParams = requestedPageWithParams.substring(0, fragmentIndex);
        }

        // Separate query parameters if present
        int paramStart = requestedPageWithParams.indexOf('?');
        if (paramStart != -1) {
            this.requestedPage = requestedPageWithParams.substring(0, paramStart);
            String paramsLine = requestedPageWithParams.substring(paramStart + 1);
            parseParams(paramsLine); // Parse GET parameters
        } else {
            this.requestedPage = requestedPageWithParams;
        }
        
        if (requestBody.length() > 0) parseParams(requestBody);
        // Check if requested page is an image
        this.isImage = requestedPage.matches(".*\\.(jpg|bmp|gif|png)$");
        // Extract content length
        chunked = false;
        for (String line : lines) {
            if (line.startsWith("Content-Length: ")) {
                this.contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
            } else if (line.startsWith("Referer: ")) {
                this.referer = line.substring("Referer: ".length());
            } else if (line.startsWith("User-Agent: ")) {
                this.userAgent = line.substring("User-Agent: ".length());
            } else if (line.toLowerCase().startsWith("chunked: ")) { // Case-insensitive match
                chunked = "yes".equalsIgnoreCase(line.substring("Chunked: ".length()));
            }
        }

    }

    // Getters
    public String getType() {
        return type;
    }

    public String getRequestedPage() {
        return requestedPage;
    }

    public boolean isImage() {
        return isImage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }
    public boolean getBadRequest() {
        return badRequest;
    }
    public boolean getChunked() {
        return chunked;
    }

    public HashMap<String, String> getParameters() {
        return parameters;
    }
    private void parseParams(String paramsLine) {
        String[] params = paramsLine.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    this.parameters.put(key, value);
                } catch (Exception e) {
                    System.err.println("Error decoding parameter: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
    }
}
