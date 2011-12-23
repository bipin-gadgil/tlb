package tlb.server.repo.model;

import tlb.domain.PartitionIdentifier;
import tlb.domain.SuiteNamePartitionEntry;
import tlb.server.repo.PartitionRecordRepo;
import tlb.server.repo.SetRepo;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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

    public SetRepo.OperationResult reportSubset(int partitionNumber, int totalPartitions, final Reader reader) throws IOException {
        SetRepo.OperationResult operationResult = universalSetRepo.usedBySubset(partitionNumber, totalPartitions, reader);
        partitionRecordRepo.subsetReceivedFromPartition(new PartitionIdentifier(partitionNumber, totalPartitions));
        if (partitionRecordRepo.allSubsetsReceivedWithConsistentConfiguration(operationResult)) {
            List<SuiteNamePartitionEntry> unassignedSuites = new ArrayList<SuiteNamePartitionEntry>();
            for (SuiteNamePartitionEntry persistentEntry : universalSetRepo.list()) {
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
