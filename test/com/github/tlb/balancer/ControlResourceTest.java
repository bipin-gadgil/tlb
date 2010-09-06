package com.github.tlb.balancer;

import com.github.tlb.Main;
import com.github.tlb.TlbConstants;
import com.github.tlb.utils.FileUtil;
import com.github.tlb.utils.SystemEnvironment;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ControlResourceTest {
    protected Request req;
    protected Response res;
    protected ControlResource resource;
    protected HashMap<String, Object> attributes;

    @Before
    public void setUp() {
        req = mock(Request.class);
        res = mock(Response.class);
        attributes = new HashMap<String, Object>();
        when(req.getAttributes()).thenReturn(attributes);
        resource = new ControlResource(new Context(), req, res);
    }

    @Test
    public void shouldReturnRunningAsProcessStatus() throws ResourceException, IOException {
        attributes.put(TlbConstants.Balancer.QUERY, ControlResource.Query.status.toString());
        final Representation rep = resource.represent(new Variant(MediaType.TEXT_PLAIN));
        assertThat(rep.getText(), is("RUNNING"));
    }

    @Test
    public void shouldBombWhenQueryNotUnderstood() {
        attributes.put(TlbConstants.Balancer.QUERY, "foo");
        try {
            resource.represent(new Variant(MediaType.TEXT_PLAIN));
            fail("should have bombed, because 'foo' is not a known query");
        } catch (Exception e) {
            assertThat(e, is(IllegalArgumentException.class));
        }
    }

    @Test
    public void shouldAllowGet() {
        assertThat(resource.allowGet(), is(true));
    }

    @Test
    public void shouldStopServer() throws InterruptedException, IOException {
        final String port = unpriviledgedPort();
        final File buildFile = new File("build.xml");
        if (!buildFile.exists()) {
            throw new RuntimeException("please execute this test from project root");
        }
        final Process process = new ProcessBuilder("ant", "run_balancer", "-Drandom.balancer.port=" + port).start();
        final boolean[] shouldRun = new boolean[1];
        final Runnable streamPumper = new Runnable() {
            public void run() {
                shouldRun[0] = true;
                while (shouldRun[0]) {
                    emptyStream(process.getErrorStream());
                    emptyStream(process.getInputStream());
                }
            }
        };
        new Thread(streamPumper).start();

        final HttpClient client = new HttpClient(new HttpClientParams());
        final GetMethod suicide = new GetMethod(String.format("http://localhost:%s/control/suicide", port));
        final GetMethod waitForStart = new GetMethod(String.format("http://localhost:%s/control/status", port));

        while (true) {
            try {
                client.executeMethod(waitForStart);
                if (new String(waitForStart.getResponseBody()).equals("RUNNING")) break;
            } catch (IOException e) {
                //ignore
            }
        }
        try {
            client.executeMethod(suicide);
        } catch (IOException e) {
            //ignore
        }
        assertThat(process.waitFor(), is(0));
        shouldRun[0] = false;
    }

    private void emptyStream(InputStream stream) {
        try {
            final int n = stream.available();
            final byte[] bytes = new byte[n];
            if (n > 0) {
                stream.read(bytes);
                System.out.println(new String(bytes));
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private String unpriviledgedPort() {
        int unprivilegedPort = new Random(new Date().getTime()).nextInt(15);
        if (unprivilegedPort <= 1024) {
            unprivilegedPort += 1024;
        }
        return String.valueOf(unprivilegedPort);
    }
}
