package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import tlb.TlbConstants;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.resources.TlbResource;
import tlb.server.resources.VersionedResource;

import java.io.IOException;

/**
 * @understands maintaining universal set for correctness checks
 */
public class UpdateUniversalSetResource extends TlbResource {
    public UpdateUniversalSetResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected EntryRepo getRepo(EntryRepoFactory repoFactory, String key) throws IOException, ClassNotFoundException {
        throw repoFactory.createSubsetRepo(key, strAttr(TlbConstants.Server.LISTING_VERSION));
    }

    @Override
    protected Entry parseEntry(Representation entity) throws IOException {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
