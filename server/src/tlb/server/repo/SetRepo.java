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
        Map<String, SuiteNameCountEntry.PartitionIdentifier> alreadySelectedByOtherPartitions = new HashMap<String, SuiteNameCountEntry.PartitionIdentifier>();
        List<SuiteNameCountEntry> subsetSuites = parse(suiteNames);
        for (SuiteNameCountEntry subsetEntry : subsetSuites) {
            SuiteNameCountEntry persistentEntry = suiteData.get(getKey(subsetEntry));
            if (persistentEntry != null) {
                String key = getKey(persistentEntry);
                synchronized (EntryRepoFactory.mutex(getIdentifier() + key)) {
                    SuiteNameCountEntry.PartitionIdentifier partitionIdentifier = new SuiteNameCountEntry.PartitionIdentifier(partitionNumber, totalPartitions);
                    if (persistentEntry.isUsedByPartitionOtherThan(partitionIdentifier)) {
                        alreadySelectedByOtherPartitions.put(key, persistentEntry.getPartitionIdentifier());
                    } else {
                        persistentEntry.markUsedBy(partitionIdentifier);
                    }
                }
                Integer count = occurrenceCount.get(key);
                occurrenceCount.put(key, count == null ? 1 : ++count);
                } else {
                unknownSuites.add(subsetEntry);
            }
        }
        return computeResult(partitionNumber, totalPartitions, unknownSuites, occurrenceCount, alreadySelectedByOtherPartitions, subsetSuites);
    }

    private OperationResult computeResult(int partitionNumber, int totalPartitions, List<SuiteNameCountEntry> unknownSuites, Map<String, Integer> occurrenceCount, Map<String, SuiteNameCountEntry.PartitionIdentifier> alreadySelectedByOtherPartitions, List<SuiteNameCountEntry> subsetSuites) {
        ArrayList<String> nonRepeatedOccurrenceCountKeys = new ArrayList<String>();
        for (Map.Entry<String, Integer> occurrenceCountEntry : occurrenceCount.entrySet()) {
            if (occurrenceCountEntry.getValue() == 1) {
                nonRepeatedOccurrenceCountKeys.add(occurrenceCountEntry.getKey());
            }
        }
        for (String nonRepeatedKey : nonRepeatedOccurrenceCountKeys) {
            occurrenceCount.remove(nonRepeatedKey);
        }

        if (unknownSuites.isEmpty() && occurrenceCount.isEmpty() && alreadySelectedByOtherPartitions.isEmpty()) {
            return new OperationResult(true);
        }

        OperationResult failureResult = new OperationResult(false);
        if (!alreadySelectedByOtherPartitions.isEmpty()) {
            failureResult.append(String.format("- Mutual exclusion of test-suites across splits violated by partition %s/%s. Suites %s have already been selected for running by other partitions.", partitionNumber, totalPartitions, alreadySelectedByOtherPartitions));
        }

        if (!unknownSuites.isEmpty()) {
            Collections.sort(unknownSuites, new SuiteNameCountEntry.SuiteNameCountEntryComparator());
            failureResult.append(String.format("- Found %s unknown(not present in universal set) suite(s) named: %s.", unknownSuites.size(), unknownSuites));
        }
        if (!occurrenceCount.isEmpty()) {
            failureResult.append(String.format("- Found more than one occurrence of %s suite(s) named: %s.", occurrenceCount.size(), occurrenceCount));
        }
        List<SuiteNameCountEntry> univSet = sortedList();
        Collections.sort(subsetSuites, new SuiteNameCountEntry.SuiteNameCountEntryComparator());
        failureResult.append(String.format("Had total of %s suites named %s in partition %s of %s. Corresponding universal set had a total of %s suites named %s.", subsetSuites.size(), subsetSuites, partitionNumber, totalPartitions, univSet.size(), univSet));

        return failureResult;
    }

    private <T> List<Map.Entry<String, T>> sortedMapEntries(Map<String, T> occurrenceCount) {
        List<Map.Entry<String, T>> entries = new ArrayList<Map.Entry<String, T>>(occurrenceCount.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, T>>() {
            public int compare(Map.Entry<String, T> o1, Map.Entry<String, T> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return entries;
    }

    public static class OperationResult {
        public final boolean success;
        private final List<String> messages;

        public OperationResult(boolean success) {
            this.success = success;
            this.messages = new ArrayList<String>();
        }

        public OperationResult(boolean success, String message) {
            this(success);
            this.messages.add(message);
        }

        public String getMessage() {
            StringBuilder strBldr = new StringBuilder();
            for (String message : messages) {
                strBldr.append(message).append("\n");
            }
            return strBldr.toString();
        }


        public void append(String message) {
            messages.add(message);
        }
    }
}
