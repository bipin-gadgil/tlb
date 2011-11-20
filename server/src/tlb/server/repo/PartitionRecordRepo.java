package tlb.server.repo;

import tlb.domain.PartitionIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @understands persisting records of which subsets have contacted the server for correctness check
 */
public class PartitionRecordRepo extends NamedEntryRepo<PartitionIdentifier> {
    public void subsetReceivedFromPartition(PartitionIdentifier partitionIdentifier) {
        update(partitionIdentifier);
    }

    public boolean allSubsetsReceivedWithConsistentConfiguration(SetRepo.OperationResult operationResult) {
        Collection<PartitionIdentifier> list = list();
        int totalPartitions = list.iterator().next().totalPartitions;
        boolean[] partitionsReceived = new boolean[totalPartitions];
        for (PartitionIdentifier partitionIdentifier : list) {
            if (totalPartitions != partitionIdentifier.totalPartitions) {
                operationResult.appendErrorDescription(String.format("Partitions %s are being run with inconsistent total-partitions configuration. This may lead to violation of mutual-exclusion or collective-exhaustion or both. Total partitions value should be the same across all partitions of a job-name and job-version combination.", list));
                operationResult.setSuccess(false);
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

    public boolean checkAllPartitionsExecuted(SetRepo.OperationResult operationResult) {
        Collection<PartitionIdentifier> list = list();
        int totalPartitions = list.iterator().next().totalPartitions;
        boolean[] partitionsReceived = new boolean[totalPartitions];
        for (PartitionIdentifier partitionIdentifier : list) {
            partitionsReceived[partitionIdentifier.partitionNumber - 1] = true;
        }
        List<Integer> partitionsNotRun = new ArrayList<Integer>();
        for (int i = 0; i < partitionsReceived.length; i++) {
            if (! partitionsReceived[i]) {
                partitionsNotRun.add(i + 1);
            }
        }
        if (partitionsNotRun.size() > 0) {
            operationResult.setSuccess(false);
            operationResult.appendErrorDescription(String.format("%s of total %s partition(s) were not executed. This violates collective exhaustion. Please check your partition configuration for potential mismatch in total-partitions value and actual 'number of partitions' configured and check your build process triggering mechanism for failures.", partitionsNotRun, totalPartitions));
        } else {
            operationResult.appendContext("All partitions executed.");
        }
        return operationResult.isSuccess();
    }
}