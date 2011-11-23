package tlb.balancer;

import com.noelios.restlet.http.HttpConstants;
import org.apache.log4j.Logger;
import org.restlet.data.*;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.orderer.TestOrderer;
import tlb.splitter.TestSplitter;
import org.restlet.Context;
import org.restlet.resource.*;
import tlb.splitter.correctness.IncorrectBalancingException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


/**
 * @understands subseting and ordering of set of suite names given
 */
public class BalancerResource extends Resource {
    private static final Logger logger = Logger.getLogger(BalancerResource.class.getName());

    private final TestOrderer orderer;
    private final TestSplitter splitter;

    public BalancerResource(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
        orderer = (TestOrderer) context.getAttributes().get(TlbClient.ORDERER);
        splitter = (TestSplitter) context.getAttributes().get(TlbClient.SPLITTER);
    }

    @Override
    public void acceptRepresentation(Representation representation) throws ResourceException {
        List<TlbSuiteFile> suiteFiles = null;
        try {
            suiteFiles = TlbSuiteFileImpl.parse(representation.getText());
        } catch (IOException e) {
            final String message = "failed to read request";
            logger.warn(message, e);
            throw new RuntimeException(message, e);
        }
        String moduleName = header(TlbConstants.Balancer.TLB_MODULE_NAME_HEADER, TlbConstants.Balancer.DEFAULT_MODULE_NAME);
        List<TlbSuiteFile> suiteFilesSubset = null;
        try {
            suiteFilesSubset = splitter.filterSuites(suiteFiles, moduleName);
        } catch (IncorrectBalancingException e) {
            setExceptionInResponse(e, Status.CLIENT_ERROR_EXPECTATION_FAILED);
            return;
        } catch (UnsupportedOperationException e) {
            setExceptionInResponse(e, Status.SERVER_ERROR_NOT_IMPLEMENTED);
            return;
        }
        Collections.sort(suiteFilesSubset, orderer);
        final StringBuilder builder = new StringBuilder();
        for (TlbSuiteFile suiteFile : suiteFilesSubset) {
            builder.append(suiteFile.dump());
        }
        getResponse().setEntity(new StringRepresentation(builder));
    }

    private void setExceptionInResponse(RuntimeException e, final Status status) {
        getResponse().setStatus(status);
        getResponse().setEntity(new StringRepresentation(e.getMessage()));
    }

    protected String header(final String headerName, final String defaultValue) {
        return ((Form) getRequest().getAttributes().get(HttpConstants.ATTRIBUTE_HEADERS)).getFirstValue(headerName, defaultValue);
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }
}
