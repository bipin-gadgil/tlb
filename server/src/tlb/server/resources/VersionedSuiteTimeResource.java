package tlb.server.resources;

import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import tlb.server.repo.SuiteTimeRepo;

import java.io.IOException;

/**
 * @understands versioned run time of suite reported by job
 */
public class VersionedSuiteTimeResource extends VersionedResource<SuiteTimeRepo> {
    public VersionedSuiteTimeResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected SuiteTimeRepo getRepo(EntryRepoFactory repoFactory, String namespace) throws ClassNotFoundException, IOException {
        return repoFactory.createSuiteTimeRepo(namespace, EntryRepoFactory.LATEST_VERSION);
    }
}