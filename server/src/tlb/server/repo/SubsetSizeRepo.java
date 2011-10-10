package tlb.server.repo;

import tlb.domain.SubsetSizeEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @understands storage and retrival of size of subset of total suites run by job
 */
public class SubsetSizeRepo implements EntryRepo<SubsetSizeEntry> {
    private volatile List<SubsetSizeEntry> entries;
    private volatile boolean dirty;

    public SubsetSizeRepo() {
        setEntries(new ArrayList<SubsetSizeEntry>());
    }

    private synchronized void setEntries(final List<SubsetSizeEntry> list) {
        entries = Collections.synchronizedList(list);
    }

    public Collection<SubsetSizeEntry> list() {
        return Collections.unmodifiableList(entries);
    }

    public Collection<SubsetSizeEntry> list(String version) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("versioning not allowed");
    }

    public void update(SubsetSizeEntry entry) {
        throw new UnsupportedOperationException("update not allowed on repository");
    }

    public synchronized String diskDump() {
        StringBuilder dumpBuffer = new StringBuilder();
        for (SubsetSizeEntry entry : entries) {
            dumpBuffer.append(entry.dump());
        }
        dirty = false;
        return dumpBuffer.toString();
    }

    public synchronized void load(final String fileContents) {
        setEntries(parse(fileContents));
        dirty = false;
    }

    public synchronized void add(SubsetSizeEntry entry) {
        entries.add(entry);
        dirty = true;
    }

    public void setFactory(EntryRepoFactory factory) {
        //doesn't need
    }

    public void setNamespace(String namespace) {
        //doesn't need
    }

    public void setIdentifier(String type) {
        //doesn't need
    }

    public List<SubsetSizeEntry> parse(String string) {
        return SubsetSizeEntry.parse(string);
    }

    public boolean isDirty() {
        return dirty;
    }
}
