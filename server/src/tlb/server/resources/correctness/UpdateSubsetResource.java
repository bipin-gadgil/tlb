package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.domain.SuiteNamePartitionEntry;
import tlb.server.repo.PartitionRecordRepo;
import tlb.server.repo.SetRepo;
import tlb.server.repo.model.SubsetCorrectnessChecker;

import java.io.IOException;

import static tlb.TlbConstants.Server.JOB_NUMBER;
import static tlb.TlbConstants.Server.TOTAL_JOBS;

/**
 * @understands checking subsets against universal set data for correctness
 */
public class UpdateSubsetResource extends SetResource {

    public static final SuiteNamePartitionEntry.SuiteNameCountEntryComparator NAME_ENTRY_COMPARATOR = new SuiteNamePartitionEntry.SuiteNameCountEntryComparator();
    private SubsetCorrectnessChecker subsetCorrectnessChecker;

    public UpdateSubsetResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected void createRepos() throws IOException, ClassNotFoundException {
        super.createRepos();
        PartitionRecordRepo partitionRecordRepo = repoFactory().createPartitionRecordRepo(reqNamespace(), reqVersion(), reqModuleName());
        subsetCorrectnessChecker = new SubsetCorrectnessChecker(universalSetRepo, partitionRecordRepo);
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (! universalSetRepo.isPrimed()) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
            getResponse().setEntity(new StringRepresentation("Universal set for given job-name, job-version and module-name combination doesn't exist."));
            return;
        }
        SetRepo.OperationResult result = subsetCorrectnessChecker.reportSubset(reqPayload(entity), Integer.parseInt(strAttr(JOB_NUMBER)), Integer.parseInt(strAttr(TOTAL_JOBS)));
        if (result.isSuccess()) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
            getResponse().setEntity(new StringRepresentation(result.getMessage()));
        }
    }
}
