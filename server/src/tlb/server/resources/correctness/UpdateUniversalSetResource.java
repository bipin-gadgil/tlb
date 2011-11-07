package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.TlbConstants;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;
import tlb.server.resources.TlbResource;

import java.io.IOException;

/**
 * @understands maintaining universal set for correctness checks
 */
public class UpdateUniversalSetResource extends TlbResource<SetRepo> {
    public UpdateUniversalSetResource(Context context, Request request, Response response) {
        super(context, request, response);
        setModifiable(false);
        setReadable(false);
    }

    @Override
    protected SetRepo getRepo(EntryRepoFactory repoFactory, String namespace) throws IOException, ClassNotFoundException {
        return repoFactory.createUniversalSetRepo(namespace, strAttr(TlbConstants.Server.LISTING_VERSION), "module-name");
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (! repo.isPrimed()) {
            synchronized (EntryRepoFactory.repoId(repo.getIdentifier())) {
                if (! repo.isPrimed()) {
                    repo.load(reqPayload(entity));
                    getResponse().setStatus(Status.SUCCESS_CREATED);
                    return;
                }
            }
        }
        SetRepo.Match match = repo.tryMatching(reqPayload(entity));
        if (match.matched) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
            getResponse().setEntity(new StringRepresentation(match.message));
        }
    }

    private String reqPayload(Representation entity) {
        String text = null;
        try {
            text = entity.getText();
        } catch (IOException e) {
            //TODO: figure out what we have been doing in other resources
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
