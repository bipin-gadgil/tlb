package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;
import tlb.utils.Function;
import tlb.utils.Procedure;

import java.io.IOException;
import java.io.Reader;

import static tlb.TlbConstants.Correctness.CURRENT_PARTITION_POSTED_INCORRECT_UNIVERSAL_SET;

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
                    synchronized (universalSetRepo) {
                        reqPayload(new Procedure<Reader, IOException>() {
                            public void perform(Reader reader) throws IOException {
                                universalSetRepo.loadAndMarkDirty(reader);
                            }
                        }, entity);
                    }
                    getResponse().setStatus(new Status(Status.SUCCESS_CREATED, "First definition of universal set stored"));
                    return;
                }
            }
        }
        SetRepo.OperationResult match = reqPayload(new Function<Reader, IOException, SetRepo.OperationResult>() {
            public SetRepo.OperationResult execute(Reader reader) throws IOException {
                return universalSetRepo.tryMatching(reader);
            }
        }, entity);
        if (match.isSuccess()) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(new Status(Status.CLIENT_ERROR_CONFLICT, CURRENT_PARTITION_POSTED_INCORRECT_UNIVERSAL_SET));
            getResponse().setEntity(new StringRepresentation(match.getMessage()));
        }
    }
}
