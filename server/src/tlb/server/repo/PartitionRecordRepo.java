package tlb.server.repo;

import tlb.domain.PartitionIdentifier;

import java.util.Collection;
import java.util.List;

/**
 * @understands persisting records of which subsets have contacted the server for correctness check
 */
public class PartitionRecordRepo extends NamedEntryRepo<PartitionIdentifier> {
    public void subsetReceivedFromPartition(PartitionIdentifier partitionIdentifier) {
        nameToEntry.put(getKey(partitionIdentifier), partitionIdentifier);
    }

    public boolean allSubsetsReceived(SetRepo.OperationResult operationResult) {
        int totalPartitions = -1;
        Collection<PartitionIdentifier> list = list();
        boolean[] partitionsReceived = new boolean[list.iterator().next().totalPartitions];
        for (PartitionIdentifier partitionIdentifier : list) {
            if (totalPartitions == -1) {
                totalPartitions = partitionIdentifier.totalPartitions;
            } else {
                if (totalPartitions != partitionIdentifier.totalPartitions) {
                    operationResult.appendErrorDescription(String.format("Partitions %s are being run with inconsistent total-partitions configuration. This may lead to violation of mutual-exclusion or collective-exhaustion or both. Total partitions value should be the same across all partitions of a job-name and job-version combination.", list));
                    operationResult.setSuccess(false);
                }
            }
            partitionsReceived[partitionIdentifier.partitionNumber - 1] = true;
        }
        for (boolean b : partitionsReceived) {
            if (! b) {
                return false;
            }
        }
        return true;
    }

    public List<PartitionIdentifier> parse(String partitionIdsString) {
        return PartitionIdentifier.parse(partitionIdsString);
    }
}
