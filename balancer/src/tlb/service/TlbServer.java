package tlb.service;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.service.http.DefaultHttpAction;
import tlb.service.http.HttpAction;
import tlb.splitter.correctness.ValidationResult;
import tlb.utils.SystemEnvironment;

import java.io.IOException;
import java.util.List;

import static tlb.TlbConstants.Server.EntryRepoFactory.*;
import static tlb.TlbConstants.Server.EntryRepoFactory.CORRECTNESS_CHECK;
import static tlb.TlbConstants.TlbServer.TLB_JOB_NAME;
import static tlb.TlbConstants.TlbServer.TLB_BASE_URL;
import static tlb.TlbConstants.TlbServer.TLB_JOB_VERSION;

/**
 * @understands exchanging balancing/ordering related data with the TLB server
 */
public class TlbServer extends SmoothingServer {
    private final HttpAction httpAction;

    //reflectively invoked by factory
    public TlbServer(SystemEnvironment systemEnvironment) {
        this(systemEnvironment, new DefaultHttpAction());
    }

    public TlbServer(SystemEnvironment systemEnvironment, HttpAction httpAction) {
        super(systemEnvironment);
        this.httpAction = httpAction;
    }

    private String suiteTimeRepoName() {
        return SUITE_TIME;
    }

    @Override
    protected void postFailedTestsToServer(List<SuiteResultEntry> resultEntries) {
        StringBuilder builder = new StringBuilder();
        for (SuiteResultEntry resultEntry : resultEntries) {
            builder.append(resultEntry.dump());
        }
        httpAction.put(suiteResultUrl(), builder.toString());
    }

    public List<SuiteTimeEntry> fetchLastRunTestTimes() {
        return SuiteTimeEntry.parse(httpAction.get(getUrl(namespace(), suiteTimeRepoName(), jobVersion())));
    }

    public List<SuiteResultEntry> getLastRunFailedTests() {
        return SuiteResultEntry.parse(httpAction.get(suiteResultUrl()));
    }

    @Override
    protected void postSubsetSizeToServer(int size) {
        httpAction.post(getUrl(jobName(), SUBSET_SIZE), String.valueOf(size));
    }

    @Override
    protected void postTestTimesToServer(String body) {
        httpAction.put(getUrl(namespace(), suiteTimeRepoName()), body);
    }

    public void clearOtherCachingFiles() {
        //NOOP
        //TODO: if chattiness becomes a problem, this will need to be implemented sensibly
    }

    public int partitionNumber() {
        return Integer.parseInt(environment.val(new SystemEnvironment.EnvVar(TlbConstants.TlbServer.TLB_PARTITION_NUMBER)));
    }

    public int totalPartitions() {
        return Integer.parseInt(environment.val(new SystemEnvironment.EnvVar(TlbConstants.TlbServer.TLB_TOTAL_PARTITIONS)));
    }

    private static class RemoteValidationResponse {
        final int status;
        final String body;

        RemoteValidationResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    public ValidationResult validateUniversalSet(List<TlbSuiteFile> universalSet, String moduleName) {
        RemoteValidationResponse resp = correctnessPost(universalSet, getUrl(namespace(), CORRECTNESS_CHECK, jobVersion(), TlbConstants.Server.EntryRepoFactory.UNIVERSAL_SET, moduleName));

        if (resp.status == HttpStatus.SC_CREATED) {
            return new ValidationResult(ValidationResult.Status.FIRST, "First validation snapshot.");
        } else if (resp.status == HttpStatus.SC_OK) {
            return new ValidationResult(ValidationResult.Status.OK, "Universal set matched.");
        } else if (resp.status == HttpStatus.SC_CONFLICT) {
            return new ValidationResult(ValidationResult.Status.FAILED, resp.body);
        } else {
            throw new IllegalStateException(String.format("Status %s for validation request not understood.", resp.status));
        }
    }

    public ValidationResult validateSubSet(List<TlbSuiteFile> subSet, String moduleName) {
        RemoteValidationResponse resp = correctnessPost(subSet, getUrl(namespace(), CORRECTNESS_CHECK, jobVersion(), String.valueOf(totalPartitions()), String.valueOf(partitionNumber()), TlbConstants.Server.EntryRepoFactory.SUB_SET, moduleName));

        if (resp.status == HttpStatus.SC_NOT_ACCEPTABLE) {
            return new ValidationResult(ValidationResult.Status.FAILED, resp.body);
        } else if (resp.status == HttpStatus.SC_OK) {
            return new ValidationResult(ValidationResult.Status.OK, "Subset found consistent.");
        } else if (resp.status == HttpStatus.SC_CONFLICT) {
            return new ValidationResult(ValidationResult.Status.FAILED, resp.body);
        } else {
            throw new IllegalStateException(String.format("Status %s for validation request not understood.", resp.status));
        }
    }

    public ValidationResult verifyAllPartitionsExecutedFor(String moduleName) {
        HttpResponse httpResponse = httpAction.doGet(getUrl(namespace(), CORRECTNESS_CHECK, jobVersion(), TlbConstants.Server.VERIFY_PARTITION_COMPLETENESS, moduleName));
        RemoteValidationResponse resp = validationResponse(httpResponse);

        if (resp.status == HttpStatus.SC_EXPECTATION_FAILED) {
            return new ValidationResult(ValidationResult.Status.FAILED, resp.body);
        } else if (resp.status == HttpStatus.SC_OK) {
            return new ValidationResult(ValidationResult.Status.OK, resp.body);
        } else {
            throw new IllegalStateException(String.format("Status %s for validation request not understood.", resp.status));
        }
    }

    private RemoteValidationResponse correctnessPost(List<TlbSuiteFile> set, final String url) {
        StringBuilder builder = new StringBuilder();
        for (TlbSuiteFile suiteFile : set) {
            builder.append(suiteFile.dump());
        }
        HttpResponse httpResponse = httpAction.doPost(url, builder.toString());
        return validationResponse(httpResponse);
    }

    private RemoteValidationResponse validationResponse(HttpResponse httpResponse) {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        String responseBody = null;
        try {
            responseBody = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new RemoteValidationResponse(statusCode, responseBody);
    }

    private String getUrl(String... parts) {
        final StringBuilder builder = new StringBuilder();
        builder.append(environment.val(new SystemEnvironment.EnvVar(TLB_BASE_URL)));
        for (String part : parts) {
            builder.append("/").append(part);
        }
        return builder.toString();
    }

    private String suiteResultUrl() {
        return getUrl(namespace(), SUITE_RESULT);
    }

    private String jobName() {
        return String.format("%s-%s", namespace(), partitionNumber());
    }

    private String namespace() {
        return environment.val(new SystemEnvironment.EnvVar(TLB_JOB_NAME));
    }

    private String jobVersion() {
        return environment.val(new SystemEnvironment.EnvVar(TLB_JOB_VERSION));
    }
}
