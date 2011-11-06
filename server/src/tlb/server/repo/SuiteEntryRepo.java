package tlb.server.repo;

import tlb.domain.SuiteLevelEntry;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @understands persistence and retrieval of suite based data
 */
public abstract class SuiteEntryRepo<T extends SuiteLevelEntry> implements EntryRepo<T> {
    protected volatile Map<String, T> suiteData;
    protected String namespace;
    transient protected EntryRepoFactory factory;
    protected volatile String identifier;
    private volatile boolean dirty;

    public SuiteEntryRepo() {
        super();
        suiteData = new ConcurrentHashMap<String, T>();
    }

    public List<T> sortedList() {
        return sortedListFor(list());
    }

    protected static <T extends SuiteLevelEntry> List<T> sortedListFor(Collection<T> list) {
        List<T> entryList = new ArrayList<T>(list);
        Collections.sort(entryList, new Comparator<SuiteLevelEntry>() {
            public int compare(SuiteLevelEntry o1, SuiteLevelEntry o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return entryList;
    }

    public Collection<T> list() {
        return suiteData.values();
    }

    public synchronized void update(T record) {
        suiteData.put(record.getName(), record);
        dirty = true;
    }

    public final void add(T entry) {
        throw new UnsupportedOperationException("add not allowed on repository");
    }

    public final void setFactory(EntryRepoFactory factory) {
        this.factory = factory;
    }

    public final boolean hasFactory() {
        return factory != null;
    }

    //TODO: kill these two setters(namespace and identifier), pass data in one shot to set both internally
    public final void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setIdentifier(String type) {
        this.identifier = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isDirty() {
        return dirty;
    }

    public synchronized final String diskDump() {
        StringBuilder dumpBuffer = new StringBuilder();
        for (T entry : suiteData.values()) {
            dumpBuffer.append(entry.dump());
        }
        dirty = false;
        return dumpBuffer.toString();
    }

    public synchronized void loadCopyFromDisk(final String fileContents) {
        load(fileContents);
        dirty = false;
    }

    public void load(String contents) {
        suiteData.clear();
        for (T entry : parse(contents)) {
            suiteData.put(entry.getName(), entry);
        }
        dirty = true;
    }
}
