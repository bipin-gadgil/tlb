package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import tlb.server.repo.PartitionRecordRepo;
import tlb.server.repo.SetRepo;
import tlb.server.resources.TlbResource;

import java.io.IOException;

/**
 * @understands verifying all partitions have run for a job-name + job-version + module-name combination
 */
public class VerifyPartitionCompletenessResource extends TlbResource {
    private PartitionRecordRepo partitionRecordRepo;

    public VerifyPartitionCompletenessResource(Context context, Request request, Response response) {
        super(context, request, response);
        setModifiable(false);
        setReadable(false);
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    @Override
    protected void createRepos() throws IOException, ClassNotFoundException {
        partitionRecordRepo = repoFactory().createPartitionRecordRepo(reqNamespace(), reqVersion(), reqModuleName());
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        SetRepo.OperationResult operationResult = new SetRepo.OperationResult(true);
        if (partitionRecordRepo.checkAllPartitionsExecuted(operationResult)) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED);
        }
        return new StringRepresentation(operationResult.getMessage());
    }
}
