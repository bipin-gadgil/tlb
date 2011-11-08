package tlb.balancer;

import com.noelios.restlet.http.HttpConstants;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.util.WrapperResponse;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.orderer.TestOrderer;
import tlb.splitter.AbstractTestSplitter;
import tlb.splitter.TestSplitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class BalancerResourceTest {

    private BalancerResource balancerResource;
    private TestOrderer orderer;
    protected TestSplitter criteria;
    protected Request request;
    protected TestUtil.LogFixture logFixture;
    protected Response response;
    protected Representation representation;

    private HashMap<String,Object> attrs;

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        Context context = new Context();

        request = mock(Request.class);
        attrs = new HashMap<String, Object>();
        attrs.put(HttpConstants.ATTRIBUTE_HEADERS, new Form());
        when(request.getAttributes()).thenReturn(attrs);

        criteria = mock(AbstractTestSplitter.class);
        orderer = mock(TestOrderer.class);

        final HashMap<String, Object> ctxMap = new HashMap<String, Object>();
        ctxMap.put(TlbClient.SPLITTER, criteria);
        ctxMap.put(TlbClient.ORDERER, orderer);
        context.setAttributes(ctxMap);

        response = new WrapperResponse(mock(Response.class)) {
            @Override
            public void setEntity(Representation entity) {
                representation = entity;
            }
        };

        balancerResource = new BalancerResource(context, request, response);

        logFixture = new TestUtil.LogFixture();
    }

    @Test
    public void shouldListFilteredAndOrderedSubsetOfTlbTestSuitesProvided() throws ResourceException, IOException {
        when(criteria.filterSuites(new ArrayList<TlbSuiteFile>(Arrays.asList(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Bang.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"))), "module_foo"))
                .thenReturn(new ArrayList<TlbSuiteFile>(Arrays.asList(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"))));
        when(orderer.compare(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"))).thenReturn(1);

        attrs.put(HttpConstants.ATTRIBUTE_HEADERS, new Form(Arrays.asList(new Parameter(TlbConstants.Balancer.TLB_MODULE_NAME_HEADER, "module_foo"))));

        balancerResource.acceptRepresentation(new StringRepresentation("foo/bar/Baz.class\nfoo/bar/Bang.class\nfoo/bar/Quux.class\n"));

        verify(criteria).filterSuites(new ArrayList<TlbSuiteFile>(Arrays.asList(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Bang.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"))), "module_foo");
        verify(orderer).compare(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"));
        assertThat(representation.getText(), is("foo/bar/Quux.class\nfoo/bar/Baz.class\n"));
    }

    @Test
    public void shouldListFilteredAndOrderedSubsetOfTlbTestSuitesProvided_WithDefaultModuleNameWhenNoneGiven() throws ResourceException, IOException {
        when(criteria.filterSuites(new ArrayList<TlbSuiteFile>(Arrays.asList(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Bang.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"))), TlbConstants.Balancer.DEFAULT_MODULE_NAME))
                .thenReturn(new ArrayList<TlbSuiteFile>(Arrays.asList(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"))));
        when(orderer.compare(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"))).thenReturn(1);

        balancerResource.acceptRepresentation(new StringRepresentation("foo/bar/Baz.class\nfoo/bar/Bang.class\nfoo/bar/Quux.class\n"));

        verify(criteria).filterSuites(new ArrayList<TlbSuiteFile>(Arrays.asList(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Bang.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"))), TlbConstants.Balancer.DEFAULT_MODULE_NAME);
        verify(orderer).compare(new TlbSuiteFileImpl("foo/bar/Baz.class"), new TlbSuiteFileImpl("foo/bar/Quux.class"));
        assertThat(representation.getText(), is("foo/bar/Quux.class\nfoo/bar/Baz.class\n"));
    }

    @Test
    public void shouldPropagateExceptionAndLogWhenFailsToGetRequestBody() throws ResourceException, IOException {
        logFixture.startListening();
        final IOException requestReadException = new IOException("test exception");
        final Representation representation = mock(Representation.class);
        when(representation.getText()).thenThrow(requestReadException);
        try {
            balancerResource.acceptRepresentation(representation);
            fail("should have exceptioned");
        } catch(RuntimeException e) {
            assertThat(e.getMessage(), is("failed to read request"));
            assertThat(e.getCause(), is((Throwable) requestReadException));
        }
        logFixture.stopListening();
        logFixture.assertHeard("failed to read request");
        logFixture.assertHeardException(requestReadException);
    }
    
    @Test
    public void shouldAcceptPlainText() {
        assertThat(balancerResource.getVariants().get(0).getMediaType(), is(MediaType.TEXT_PLAIN));
    }

    @Test
    public void shouldNotAllowGet() {
        assertThat(balancerResource.allowGet(), is(false));
    }

    @Test
    public void shouldAllowPost() {
        assertThat(balancerResource.allowPost(), is(true));
    }
}
