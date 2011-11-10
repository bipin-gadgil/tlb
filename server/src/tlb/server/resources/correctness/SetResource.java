package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import tlb.TlbConstants;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;
import tlb.server.resources.TlbResource;

import java.io.IOException;

/**
 * @understands common features of a resource dealing with immutable sets of suites
 */
public abstract class SetResource extends TlbResource<SetRepo> {
    public SetResource(Context context, Request request, Response response) {
        super(context, request, response);
        setModifiable(false);
        setReadable(false);
    }

    @Override
    protected SetRepo getRepo(EntryRepoFactory repoFactory, String namespace) throws IOException, ClassNotFoundException {
        return repoFactory.createUniversalSetRepo(namespace, strAttr(TlbConstants.Server.LISTING_VERSION), strAttr(TlbConstants.Server.MODULE_NAME));
    }

    protected String reqPayload(Representation entity) {
        String text = null;
        try {
            text = entity.getText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return text;
    }

    @Override
    protected Entry parseEntry(Representation entity) throws IOException {
        throw new UnsupportedOperationException("not implemented yet");
    }


    @Override
    public boolean allowPost() {
        return true;
    }
}
