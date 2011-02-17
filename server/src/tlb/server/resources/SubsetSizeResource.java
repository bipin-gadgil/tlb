package tlb.server.resources;

import tlb.domain.Entry;
import tlb.domain.SubsetSizeEntry;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;

import java.io.IOException;

/**
 * @understands subset sizes reported by a job
 */
public class SubsetSizeResource extends TlbResource {

    public SubsetSizeResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected EntryRepo getRepo(EntryRepoFactory repoFactory, String key) throws IOException, ClassNotFoundException {
        return repoFactory.createSubsetRepo(key, EntryRepoFactory.LATEST_VERSION);
    }

    @Override
    protected Entry parseEntry(Representation entity) throws IOException {
        return SubsetSizeEntry.parseSingleEntry(entity.getText());
    }

    @Override
    public boolean allowPost() {
        return true;
    }
}
