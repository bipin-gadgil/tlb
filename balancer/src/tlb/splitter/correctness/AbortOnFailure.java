package tlb.splitter.correctness;

import org.apache.log4j.Logger;
import tlb.TlbSuiteFile;
import tlb.service.Server;
import tlb.service.TalksToServer;
import tlb.splitter.TestSplitter;

import java.util.List;

/**
 * @understands aborting when split check fails
 */
public class AbortOnFailure extends SplitChecker implements TalksToServer {
    private static final Logger logger = Logger.getLogger(AbortOnFailure.class.getName());

    private Server service;

    public AbortOnFailure(final TestSplitter splitter) {
        super(splitter);
    }

    @Override
    public void universalSet(List<TlbSuiteFile> fileResources) {
        ValidationResult result = service.validateUniversalSet(fileResources);
        logger.warn(String.format("Split check result for universalSet: %s", result));
        if (result.hasFailed()) {
            throw new IllegalStateException(result.getMessage());
        }
    }

    @Override
    public void subSet(List<TlbSuiteFile> fileResources) {
        ValidationResult result = service.validateSubSet(fileResources);
        logger.warn(String.format("Split check result for subSet: %s", result));
        if (result.hasFailed()) {
            throw new IllegalStateException(result.getMessage());
        }
    }

    public void talksToServer(Server service) {
        this.service = service;
    }
}
