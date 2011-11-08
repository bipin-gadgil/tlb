package tlb.service;

import org.apache.commons.io.IOUtils;
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

import java.io.ByteArrayOutputStream;
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

    public void processedTestClassTime(String className, long time) {
        httpAction.put(getUrl(namespace(), suiteTimeRepoName()), String.format("%s: %s", className, time));
    }

    private String suiteTimeRepoName() {
        return SUITE_TIME;
    }

    public void testClassFailure(String className, boolean hasFailed) {
        httpAction.put(suiteResultUrl(), new SuiteResultEntry(className, hasFailed).toString());
    }

    public List<SuiteTimeEntry> fetchLastRunTestTimes() {
        return SuiteTimeEntry.parse(httpAction.get(getUrl(namespace(), suiteTimeRepoName(), jobVersion())));
    }

    public List<SuiteResultEntry> getLastRunFailedTests() {
        return SuiteResultEntry.parse(httpAction.get(suiteResultUrl()));
    }

    public void publishSubsetSize(int size) {
        httpAction.post(getUrl(jobName(), SUBSET_SIZE), String.valueOf(size));
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

    public ValidationResult validateUniversalSet(List<TlbSuiteFile> universalSet, String moduleName) {
        StringBuilder builder = new StringBuilder();
        for (TlbSuiteFile suiteFile : universalSet) {
            builder.append(suiteFile.dump());
        }
        HttpResponse httpResponse = httpAction.doPost(validationUrl(TlbConstants.Server.EntryRepoFactory.UNIVERSAL_SET, moduleName), builder.toString());
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        String responseBody = null;
        try {
            responseBody = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (statusCode == HttpStatus.SC_CREATED) {
            return new ValidationResult(ValidationResult.Status.FIRST, "First validation snapshot.");
        } else if (statusCode == HttpStatus.SC_OK) {
            return new ValidationResult(ValidationResult.Status.OK, "Universal set matched.");
        } else if (statusCode == HttpStatus.SC_CONFLICT) {
            return new ValidationResult(ValidationResult.Status.FAILED, responseBody);
        } else {
            throw new IllegalStateException(String.format("Status %s for validation request not understood.", statusCode));
        }
    }

    public ValidationResult validateSubSet(List<TlbSuiteFile> subSet, String moduleName) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private String getUrl(String... parts) {
        final StringBuilder builder = new StringBuilder();
        builder.append(environment.val(new SystemEnvironment.EnvVar(TLB_BASE_URL)));
        for (String part : parts) {
            builder.append("/").append(part);
        }
        return builder.toString();
    }

    private String validationUrl(String setType, String moduleName) {
        return getUrl(namespace(), CORRECTNESS_CHECK, jobVersion(), String.valueOf(totalPartitions()), String.valueOf(partitionNumber()), setType, moduleName);
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
