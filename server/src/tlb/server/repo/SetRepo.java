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

    public OperationResult tryMatching(String list) {
        List<SuiteNameCountEntry> givenList = sortedListFor(parse(list));
        List<SuiteNameCountEntry> serverCopy = sortedList();

        for (int i = 0; i < serverCopy.size(); i++) {
            SuiteNameCountEntry entry = serverCopy.get(i);
            if (! entry.equals(givenList.get(i))) {
                return new OperationResult(false, String.format("Expected universal set was %s but given %s.", serverCopy, givenList));
            }
        }
        return new OperationResult(true);
    }

    @Override
    public void update(SuiteNameCountEntry record) {
        throw new UnsupportedOperationException("not allowed on this type of repository");
    }

    public OperationResult usedBySubset(String suiteNames, int partitionNumber, int totalPartitions) {
        List<SuiteNameCountEntry> unknownSuites = new ArrayList<SuiteNameCountEntry>();
        Map<String, Integer> occurrenceCount = new HashMap<String, Integer>();
        List<SuiteNameCountEntry> subsetSuites = parse(suiteNames);
        for (SuiteNameCountEntry subsetEntry : subsetSuites) {
            SuiteNameCountEntry persistentEntry = suiteData.get(getKey(subsetEntry));
            if (persistentEntry != null) {
                String key = getKey(persistentEntry);
                synchronized (EntryRepoFactory.mutex(getIdentifier() + key)) {
                    if (persistentEntry.isUnused()) {
                        persistentEntry.usedBy(partitionNumber, totalPartitions);
                    }
                }
                Integer count = occurrenceCount.get(key);
                occurrenceCount.put(key, count == null ? 1 : ++count);
                } else {
                unknownSuites.add(subsetEntry);
            }
        }
        return computeResult(partitionNumber, totalPartitions, unknownSuites, occurrenceCount, subsetSuites);
    }

    private OperationResult computeResult(int partitionNumber, int totalPartitions, List<SuiteNameCountEntry> unknownSuites, Map<String, Integer> occurrenceCount, List<SuiteNameCountEntry> subsetSuites) {
        ArrayList<String> nonRepeatedOccurrenceCountKeys = new ArrayList<String>();
        for (Map.Entry<String, Integer> occurrenceCountEntry : occurrenceCount.entrySet()) {
            if (occurrenceCountEntry.getValue() == 1) {
                nonRepeatedOccurrenceCountKeys.add(occurrenceCountEntry.getKey());
            }
        }
        for (String nonRepeatedKey : nonRepeatedOccurrenceCountKeys) {
            occurrenceCount.remove(nonRepeatedKey);
        }

        boolean success = unknownSuites.isEmpty() && occurrenceCount.isEmpty();
        if (! success) {
            StringBuilder builder = new StringBuilder();
            if (! unknownSuites.isEmpty()) {
                Collections.sort(unknownSuites, new SuiteNameCountEntry.SuiteNameCountEntryComparator());
                builder.append(String.format("- Found %s unknown(not present in universal set) suite(s) named: %s.\n", unknownSuites.size(), unknownSuites));
            }
            if (! occurrenceCount.isEmpty()) {
                ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(occurrenceCount.entrySet());
                Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
                    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });
                builder.append(String.format("- Found more than one occurrence of %s suite(s) named: %s\n", occurrenceCount.size(), occurrenceCount));
            }
            List<SuiteNameCountEntry> univSet = sortedList();
            Collections.sort(subsetSuites, new SuiteNameCountEntry.SuiteNameCountEntryComparator());
            builder.append(String.format("Had total of %s suites named %s in partition %s of %s.\nCorresponding universal set had a total of %s suites named %s.", subsetSuites.size(), subsetSuites, partitionNumber, totalPartitions, univSet.size(), univSet));
            return new OperationResult(false, builder.toString());
        }
        return new OperationResult(true);
    }

    public static class OperationResult {
        public final boolean success;
        public final String message;

        public OperationResult(boolean success) {
            this(success, null);
        }

        public OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
