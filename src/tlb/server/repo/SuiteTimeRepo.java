package tlb.server.repo;

import tlb.domain.SuiteTimeEntry;
import tlb.domain.TimeProvider;

import java.io.IOException;

/**
 * @understands storage and retrival of time that each suite took to run
 */
public class SuiteTimeRepo extends VersioningEntryRepo<SuiteTimeEntry> {

    public SuiteTimeRepo(TimeProvider timeProvider) {
        super(timeProvider);
    }

    @Override
    public SuiteTimeRepo getSubRepo(String versionIdentifier) throws IOException, ClassNotFoundException {
        return factory.createSuiteTimeRepo(namespace, versionIdentifier);
    }

    @Override
    protected SuiteTimeEntry parseSingleEntry(String string) {
        return SuiteTimeEntry.parseSingleEntry(string);
    }
}
