package tlb.server.repo;

import tlb.domain.PartitionIdentifier;
import tlb.domain.SuiteNamePartitionEntry;

import java.io.IOException;
import java.util.*;

/**
 * @understands storing subset of test-suite names
 */
public class SetRepo extends NamedEntryRepo<SuiteNamePartitionEntry> {

    public SetRepo() {
    }

    public List<SuiteNamePartitionEntry> parse(String string) {
        return SuiteNamePartitionEntry.parse(string);
    }

    public boolean isPrimed() {
        return nameToEntry.size() > 0;
    }

    public OperationResult tryMatching(String list) {
        List<SuiteNamePartitionEntry> givenList = sortedListFor(parse(list));
        List<SuiteNamePartitionEntry> serverCopy = sortedList();

        for (int i = 0; i < serverCopy.size(); i++) {
            SuiteNamePartitionEntry entry = serverCopy.get(i);
            if (! entry.equals(givenList.get(i))) {
                return new OperationResult(false, String.format("Expected universal set was %s but given %s.", serverCopy, givenList));
            }
        }
        return new OperationResult(true);
    }

    public Collection<SuiteNamePartitionEntry> list(String version) throws IOException, ClassNotFoundException {
        return nameToEntry.values();
    }

    @Override
    public void update(SuiteNamePartitionEntry record) {
        throw new UnsupportedOperationException("not allowed on this type of repository");
    }

    public OperationResult usedBySubset(String suiteNames, int partitionNumber, int totalPartitions) {
        List<SuiteNamePartitionEntry> unknownSuites = new ArrayList<SuiteNamePartitionEntry>();
        Map<String, Integer> occurrenceCount = new HashMap<String, Integer>();
        List<SuiteNamePartitionEntry> alreadySelectedByOtherPartitions = new ArrayList<SuiteNamePartitionEntry>();
        List<SuiteNamePartitionEntry> subsetSuites = parse(suiteNames);
        for (SuiteNamePartitionEntry subsetEntry : subsetSuites) {
            SuiteNamePartitionEntry persistentEntry = nameToEntry.get(getKey(subsetEntry));
            if (persistentEntry != null) {
                String key = getKey(persistentEntry);
                synchronized (EntryRepoFactory.mutex(getIdentifier() + key)) {
                    PartitionIdentifier partitionIdentifier = new PartitionIdentifier(partitionNumber, totalPartitions);
                    if (persistentEntry.isUsedByPartitionOtherThan(partitionIdentifier)) {
                        alreadySelectedByOtherPartitions.add(persistentEntry);
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

    private OperationResult computeResult(int partitionNumber, int totalPartitions, List<SuiteNamePartitionEntry> unknownSuites, Map<String, Integer> occurrenceCount, List<SuiteNamePartitionEntry> alreadySelectedByOtherPartitions, List<SuiteNamePartitionEntry> subsetSuites) {
        ArrayList<String> nonRepeatedOccurrenceCountKeys = new ArrayList<String>();
        for (Map.Entry<String, Integer> occurrenceCountEntry : occurrenceCount.entrySet()) {
            if (occurrenceCountEntry.getValue() == 1) {
                nonRepeatedOccurrenceCountKeys.add(occurrenceCountEntry.getKey());
            }
        }
        for (String nonRepeatedKey : nonRepeatedOccurrenceCountKeys) {
            occurrenceCount.remove(nonRepeatedKey);
        }

        OperationResult failureResult = new OperationResult(unknownSuites.isEmpty() && occurrenceCount.isEmpty() && alreadySelectedByOtherPartitions.isEmpty());
        if (!alreadySelectedByOtherPartitions.isEmpty()) {
            failureResult.appendErrorDescription(String.format("Mutual exclusion of test-suites across splits violated by partition %s/%s. Suites %s have already been selected for running by other partitions.", partitionNumber, totalPartitions, alreadySelectedByOtherPartitions));
        }

        if (!unknownSuites.isEmpty()) {
            Collections.sort(unknownSuites, new SuiteNamePartitionEntry.SuiteNameCountEntryComparator());
            failureResult.appendErrorDescription(String.format("Found %s unknown(not present in universal set) suite(s) named: %s.", unknownSuites.size(), unknownSuites));
        }
        if (!occurrenceCount.isEmpty()) {
            failureResult.appendErrorDescription(String.format("Found more than one occurrence of %s suite(s) named: %s.", occurrenceCount.size(), occurrenceCount));
        }
        List<SuiteNamePartitionEntry> univSet = sortedList();
        Collections.sort(subsetSuites, new SuiteNamePartitionEntry.SuiteNameCountEntryComparator());
        failureResult.appendContext(String.format("Had total of %s suites named %s in partition %s of %s. Corresponding universal set had a total of %s suites named %s.", subsetSuites.size(), subsetSuites, partitionNumber, totalPartitions, univSet.size(), univSet));

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
        private boolean success;
        private final List<String> messages;
        private final List<String> context;

        public OperationResult(boolean success) {
            this.success = success;
            this.messages = new ArrayList<String>();
            this.context = new ArrayList<String>();
        }

        public OperationResult(boolean success, String message) {
            this(success);
            this.messages.add(message);
        }

        public String getMessage() {
            StringBuilder strBldr = new StringBuilder();
            appendMessageCollection(strBldr, messages);
            appendMessageCollection(strBldr, context);
            return strBldr.toString();
        }

        private void appendMessageCollection(StringBuilder strBldr, final List<String> messages) {
            for (String message : messages) {
                strBldr.append(message).append("\n");
            }
        }

        public void appendErrorDescription(String message) {
            messages.add("- " + message);
        }

        public void appendContext(String message) {
            context.add(message);
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            if (this.success && this.success != success) {
                this.success = false;
            }
        }
    }
}