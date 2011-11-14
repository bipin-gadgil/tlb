package tlb.server.repo.model;

import tlb.domain.PartitionIdentifier;
import tlb.domain.SuiteNameCountEntry;
import tlb.server.repo.PartitionRecordRepo;
import tlb.server.repo.SetRepo;

import java.util.ArrayList;
import java.util.List;

/**
 * @understands ensuring subset is mutually exclusive + collectively exhaustive
 */
public class SubsetCorrectnessChecker {
    private final SetRepo universalSetRepo;
    private final PartitionRecordRepo partitionRecordRepo;

    public SubsetCorrectnessChecker(SetRepo universalSetRepo, final PartitionRecordRepo partitionRecordRepo) {
        this.universalSetRepo = universalSetRepo;
        this.partitionRecordRepo = partitionRecordRepo;
    }

    public SetRepo.OperationResult reportSubset(String subsetSuites, int partitionNumber, int totalPartitions) {
        SetRepo.OperationResult operationResult = universalSetRepo.usedBySubset(subsetSuites, partitionNumber, totalPartitions);
        partitionRecordRepo.subsetReceivedFromPartition(new PartitionIdentifier(partitionNumber, totalPartitions));
        if (partitionRecordRepo.allSubsetsReceived(operationResult)) {
            List<SuiteNameCountEntry> unassignedSuites = new ArrayList<SuiteNameCountEntry>();
            for (SuiteNameCountEntry persistentEntry : universalSetRepo.list()) {
                if (! persistentEntry.isUsedByAnyPartition()) {
                    unassignedSuites.add(persistentEntry);
                }
            }
            if (! unassignedSuites.isEmpty()) {
                operationResult.appendErrorDescription(String.format("Collective exhaustion of tests violated with none of the %s partition picked running suites: %s. Failing partition %s as this is the last one to execute.", totalPartitions, SetRepo.sortedListFor(unassignedSuites), partitionNumber));
                operationResult.setSuccess(false);
            }
        }
        return operationResult;
    }
}
