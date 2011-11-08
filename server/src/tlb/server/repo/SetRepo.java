package tlb.server.repo;

import tlb.domain.SuiteNameCountEntry;
import tlb.domain.TimeProvider;

import java.io.IOException;
import java.util.*;

/**
 * @understands storing subset of test-suite names
 */
public class SetRepo extends VersioningEntryRepo<SuiteNameCountEntry> {

    public SetRepo(TimeProvider timeProvider) {
        super(timeProvider);
    }

    @Override
    public VersioningEntryRepo<SuiteNameCountEntry> getSubRepo(String versionIdentifier) throws IOException {
        return factory.createUniversalSetRepo(namespace, versionIdentifier, "module-name");
    }

    public List<SuiteNameCountEntry> parse(String string) {
        return SuiteNameCountEntry.parse(string);
    }

    public boolean isPrimed() {
        return suiteData.size() > 0;
    }

    public Match tryMatching(String list) {
        List<SuiteNameCountEntry> givenList = sortedListFor(parse(list));
        List<SuiteNameCountEntry> serverCopy = sortedList();

        for (int i = 0; i < serverCopy.size(); i++) {
            SuiteNameCountEntry entry = serverCopy.get(i);
            if (! entry.equals(givenList.get(i))) {
                return new Match(false, String.format("Expected universal set was %s but given %s.", serverCopy, givenList));
            }
        }
        return new Match(true);
    }

    @Override
    public void update(SuiteNameCountEntry record) {
        throw new UnsupportedOperationException("not allowed on this type of repository");
    }

    public static class Match {
        public final boolean matched;
        public final String message;

        public Match(boolean matched) {
            this.matched = matched;
            this.message = null;
        }

        public Match(boolean matched, String message) {
            this.matched = matched;
            this.message = message;
        }
    }
}
