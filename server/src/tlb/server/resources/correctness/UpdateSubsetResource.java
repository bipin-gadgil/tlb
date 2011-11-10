package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.domain.SuiteNameCountEntry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @understands checking subsets against universal set data for correctness
 */
public class UpdateSubsetResource extends SetResource {

    public static final SuiteNameCountEntry.SuiteNameCountEntryComparator NAME_ENTRY_COMPARATOR = new SuiteNameCountEntry.SuiteNameCountEntryComparator();

    public UpdateSubsetResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (! repo.isPrimed()) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
            getResponse().setEntity(new StringRepresentation("Universal set for given job-name, job-version and module-name combination doesn't exist."));
            return;
        }
        SetRepo.OperationResult operationResult = repo.usedBySubset(reqPayload(entity), 1, 2);
        getResponse().setStatus(Status.SUCCESS_OK);
    }
}
