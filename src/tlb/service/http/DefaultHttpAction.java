package tlb.service.http;

import tlb.service.http.request.FollowableHttpRequest;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @understands talking http
 */
public class DefaultHttpAction implements HttpAction {
    private final HttpClient client;
    private URI url;
    private boolean ssl;
    private static final Logger logger = Logger.getLogger(DefaultHttpAction.class.getName());

    public DefaultHttpAction(HttpClient client, URI url) {
        this.client = client;
        this.url = url;
        ssl = url.getScheme().equals("https");
    }

    /**
     * its important that this be done before every http call,
     * as it can be disturbed by tests running under the load balanced environment.
     *
     * Ouch! static state again.
     */
    private void reRegisterProtocol() {
        if (ssl) {
            logger.info("(Re)registering https protocol");
            Protocol.registerProtocol("https", new Protocol("https", (ProtocolSocketFactory) new PermissiveSSLProtocolSocketFactory(), url.getPort()));
        }
    }

    public synchronized int executeMethod(HttpMethodBase method) {
        try {
            reRegisterProtocol();
            logger.info(String.format("Executing http request with %s", method.getClass().getSimpleName()));
            return client.executeMethod(method);
        } catch (IOException e) {
            throw new RuntimeException("Oops! Something went wrong", e);
        }
    }

    class FollowableGetRequest extends FollowableHttpRequest {
        protected FollowableGetRequest(DefaultHttpAction action) {
            super(action);
        }

        public HttpMethodBase createMethod(String url) {
            return new GetMethod(url);
        }
    }

    class FollowablePutRequest extends FollowableHttpRequest {
        private String data;

        protected FollowablePutRequest(DefaultHttpAction action, String data) {
            super(action);
            this.data = data;
        }

        public HttpMethodBase createMethod(String url) {
            PutMethod method = new PutMethod(url);
            try {
                method.setRequestEntity(new StringRequestEntity(data, "text/plain", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return method;
        }
    }

    class FollowablePostRequest extends FollowableHttpRequest {
        private Map<String, String> data;

        protected FollowablePostRequest(DefaultHttpAction action, Map<String, String> data) {
            super(action);
            this.data = data;
        }

        public HttpMethodBase createMethod(String url) {
            PostMethod method = new PostMethod(url);
            for (Map.Entry<String, String> param : data.entrySet()) {
                method.addParameter(param.getKey(), param.getValue());
            }
            return method;
        }
    }

    class FollowableRawPostRequest extends FollowableHttpRequest {
        private String data;

        protected FollowableRawPostRequest(DefaultHttpAction action, String data) {
            super(action);
            this.data = data;
        }

        public HttpMethodBase createMethod(String url) {
            PostMethod method = new PostMethod(url);
            try {
                method.setRequestEntity(new StringRequestEntity(data, "text/plain", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return method;
        }
    }
    

    public String get(String url) {
        FollowableGetRequest request = new FollowableGetRequest(this);
        return request.executeRequest(url);
    }

    public String post(String url, Map<String,String> data) {
        FollowablePostRequest request = new FollowablePostRequest(this, data);
        return request.executeRequest(url);
    }

    public String post(String url, String data) {
        FollowableRawPostRequest request = new FollowableRawPostRequest(this, data);
        return request.executeRequest(url);
    }

    public String put(String url, String data) {
        FollowablePutRequest request = new FollowablePutRequest(this, data);
        return request.executeRequest(url);
    }
}
