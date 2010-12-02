package tlb.service.http.request;

import tlb.TestUtil;
import tlb.service.http.DefaultHttpAction;
import tlb.service.http.PermissiveSSLProtocolSocketFactory;
import tlb.utils.RetryAfter;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

public class FollowableHttpRequestTest {
    private int attemptCount;
    private TestUtil.LogFixture logFixture;

    @Before
    public void setUp() throws Exception {
        attemptCount = 0;
        logFixture = new TestUtil.LogFixture();
    }

    @After
    public void tearDown() throws Exception {
        logFixture.stopListening();
    }

    @Test
    public void shouldRetryOnTheNewLocationFor3XX() throws Exception{
        final GetMethod getMethod = mock(GetMethod.class);
        when(getMethod.getResponseBodyAsString()).thenReturn("redirected");
        when(getMethod.getResponseHeader("Location")).thenReturn(new Header("Location", "http://some_cruise:8153/cruise/redirected"));
        final GetMethod redirectedMethod = mock(GetMethod.class);
        when(redirectedMethod.getResponseBodyAsString()).thenReturn("actual body");
        HttpClient client = mock(HttpClient.class);
        DefaultHttpAction action = new DefaultHttpAction(client, new URI("http://some_cruise:8153/cruise", true));
        when(client.executeMethod(getMethod)).thenReturn(302);
        when(client.executeMethod(redirectedMethod)).thenReturn(200);
        FollowableHttpRequest req = new FollowableHttpRequest(action) {
            public HttpMethodBase createMethod(String url) {
                return url.endsWith("redirected") ? redirectedMethod : getMethod;
            }
        };
        logFixture.startListening();
        assertThat(req.executeRequest("http://some_cruise:8153/cruise"), is("actual body"));
        logFixture.assertHeard("attempting http request to http://some_cruise:8153/cruise with " + getMethod.getClass().getSimpleName());
        logFixture.assertHeard(String.format("http request to http://some_cruise:8153/cruise with %s returned 302", getMethod.getClass().getSimpleName()));
        logFixture.assertHeard("attempting http request to http://some_cruise:8153/cruise/redirected with " + redirectedMethod.getClass().getSimpleName());
        logFixture.assertHeard(String.format("http request to http://some_cruise:8153/cruise/redirected with %s returned 200", redirectedMethod.getClass().getSimpleName()));
    }

    @Test
    public void shouldRegisterProtocolBeforeFiringUpHttpsRequest() throws Exception{
        HttpClient client = new HttpClient() {
            @Override
            public int executeMethod(HttpMethod method) {
                Protocol registeredProtocol = Protocol.getProtocol("https");
                assertThat(registeredProtocol.getSocketFactory(), instanceOf(PermissiveSSLProtocolSocketFactory.class));
                return 201;
            }
        };
        DefaultHttpAction action = new DefaultHttpAction(client, new URI("https://some.host:8154/cruise", true));
        GetMethod getMethod = mock(GetMethod.class);
        logFixture.startListening();
        assertThat(action.executeMethod(getMethod), is(201));
        logFixture.assertHeard("(Re)registering https protocol");
        logFixture.assertHeard("Executing http request with " + getMethod.getClass().getSimpleName());
    }

    @Test
    public void shouldRetryOnException() throws Exception {
        HttpClient client = mock(HttpClient.class);
        URI uri = new URI("https://some.host:8154/cruise", true);
        DefaultHttpAction action = new DefaultHttpAction(client, uri) {
            @Override
            public int executeMethod(HttpMethodBase method) {
                if (attemptCount++ < 2) throw new RuntimeException("Failed");
                return 200;
            }
        };
        final GetMethod getMethod = mock(GetMethod.class);
        when(getMethod.getResponseBodyAsString()).thenReturn("foo");
        FollowableHttpRequest req = new FollowableHttpRequest(action, new RetryAfter(1, 2, 3)) {
            @Override
            public HttpMethodBase createMethod(String url) {
                return getMethod;
            }
        };
        assertThat(req.executeRequest("http://foo.bar:8154/baz"), is("foo"));
    }

    @Test
    public void shouldShouldDefaultRetryAfterToUseAFewMinutesOfInterval() throws Exception{
        FollowableHttpRequest req = new FollowableHttpRequest(mock(DefaultHttpAction.class)) {
            public HttpMethodBase createMethod(String url) {
                return new GetMethod("http://localhost:8153/cruise");
            }
        };
        List<Integer> expectedIntervals = new ArrayList<Integer>();
        expectedIntervals.add(0);
        for(int i = 0; i < 8*6; i++) {
            expectedIntervals.add(10);
        }
        assertThat(req.getRetryer().getIntervals(), is(expectedIntervals));
    }
}
