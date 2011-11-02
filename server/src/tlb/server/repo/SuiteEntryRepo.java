package tlb.server.repo;

import tlb.domain.SuiteLevelEntry;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @understands persistence and retrieval of suite based data
 */
public abstract class SuiteEntryRepo<T extends SuiteLevelEntry> implements EntryRepo<T> {
    private volatile Map<String, T> suiteData;
    protected String namespace;
    transient protected EntryRepoFactory factory;
    protected String identifier;
    private volatile boolean dirty;

    public SuiteEntryRepo() {
        super();
        suiteData = new ConcurrentHashMap<String, T>();
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

    //TODO: kill these two setters(namespace and identifier), pass data in one shot to set both internally
    public final void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setIdentifier(String type) {
        this.identifier = type;
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

    public synchronized void load(final String fileContents) {
        suiteData.clear();
        for (T entry : parse(fileContents)) {
            suiteData.put(entry.getName(), entry);
        }
        dirty = false;
    }
}
