package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;

import java.io.IOException;

/**
 * @understands maintaining universal set for correctness checks
 */
public class UpdateUniversalSetResource extends SetResource {
    public UpdateUniversalSetResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (! universalSetRepo.isPrimed()) {
            synchronized (EntryRepoFactory.mutex(universalSetRepo.getIdentifier())) {
                if (! universalSetRepo.isPrimed()) {
                    universalSetRepo.load(reqPayload(entity));
                    getResponse().setStatus(Status.SUCCESS_CREATED);
                    return;
                }
            }
        }
        SetRepo.OperationResult match = universalSetRepo.tryMatching(reqPayload(entity));
        if (match.success) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
            getResponse().setEntity(new StringRepresentation(match.message));
        }
    }
}
