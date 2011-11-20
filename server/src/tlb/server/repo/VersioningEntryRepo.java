package tlb.server.repo;

import tlb.domain.NamedEntry;
import tlb.domain.TimeProvider;

import java.io.IOException;
import java.util.*;

/**
 * @understands versions of entry list
 */
public abstract class VersioningEntryRepo<T extends NamedEntry> extends NamedEntryRepo<T> {

    public VersioningEntryRepo(TimeProvider timeProvider) {
    }

    public void purgeOldVersions(int versionLifeInDays) throws IOException {
    }

    public abstract VersioningEntryRepo<T> getSubRepo(String versionIdentifier) throws IOException;

    public Collection<T> list(String versionIdentifier) throws IOException {
        return getSubRepo(versionIdentifier).list();
    }

    public List<T> sortedList(String version) {
        try {
            return sortedListFor(list(version));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final void loadCopyFromDisk(final String fileContents) {
        super.loadCopyFromDisk(fileContents);
    }
}
