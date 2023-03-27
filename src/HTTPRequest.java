public class HTTPRequest {
        private String method;
        private String path;
        private String ifModifiedSince;

        public HTTPRequest(String request, String rootPath) {
            String[] lines = request.split("\\r?\\n");
            String[] requestLineTokens = lines[0].split(" ");
            method = requestLineTokens[0];
            path = rootPath + requestLineTokens[1];
            ifModifiedSince = null;
            for (int i = 1; i < lines.length; i++) {
                String[] headerTokens = lines[i].split(": ");
                if (headerTokens[0].equals("If-Modified-Since")) {
                    ifModifiedSince = headerTokens[1];
                    break;
                }
            }
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getIfModifiedSince() {
            return ifModifiedSince;
        }
    }
