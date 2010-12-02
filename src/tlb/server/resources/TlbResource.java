package tlb.server.resources;

import tlb.TlbConstants;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tlb.TlbConstants.Server.REQUEST_NAMESPACE;

/**
 * @understands listing and modification of tlb resource
 */
public abstract class TlbResource extends Resource {
    private static final Logger logger = Logger.getLogger(TlbResource.class.getName());
    protected EntryRepo repo;
    private final Map<String, Object> reqAttrs;

    public TlbResource(Context context, Request request, Response response) {
        super(context, request, response);
        EntryRepoFactory repoFactory = (EntryRepoFactory) context.getAttributes().get(TlbConstants.Server.REPO_FACTORY);
        reqAttrs = request.getAttributes();
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
        String key = strAttr(REQUEST_NAMESPACE);
        try {
            repo = getRepo(repoFactory, key);
        } catch (Exception e) {
            logger.log(Level.WARNING, String.format("Failed to get repo for '%s'", key), e);
            throw new RuntimeException(e);
        }
    }

    protected String strAttr(final String requestNamespace) {
        return (String) reqAttrs.get(requestNamespace);
    }

    protected Collection<Entry> getListing() throws IOException, ClassNotFoundException {
        return repo.list();
    }

    protected abstract EntryRepo getRepo(EntryRepoFactory repoFactory, String key) throws IOException, ClassNotFoundException;

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        StringBuilder builder = new StringBuilder();
        final Collection<Entry> listing;
        try {
            listing = getListing();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (Entry entry : listing) {
            builder.append(entry.dump());
        }
        return new StringRepresentation(builder.toString(), MediaType.TEXT_PLAIN);
    }

    @Override
    public void storeRepresentation(Representation entity) throws ResourceException {
        try {
            repo.update(parseEntry(entity));
        } catch (Exception e) {
            logger.log(Level.WARNING, String.format("update of representation failed for %s", entity), e);
            throw new RuntimeException(e);
        }
    }

    protected abstract Entry parseEntry(Representation entity) throws IOException;

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        try {
            repo.add(parseEntry(entity));
        } catch (Exception e) {
            logger.log(Level.WARNING, String.format("addition of representation failed for %s", entity), e);
            throw new RuntimeException(e);
        }
    }
}
