package tlb.server.repo.model;

import tlb.server.repo.SetRepo;

/**
 * @understands ensuring subset is mutually exclusive + collectively exhaustive
 */
public class SubsetCorrectnessChecker {
    private final SetRepo universalSetRepo;

    public SubsetCorrectnessChecker(SetRepo universalSetRepo) {
        this.universalSetRepo = universalSetRepo;
    }

    public SetRepo.OperationResult reportSubset(String subsetSuites, int partitionNumber, int totalPartitions) {
        return universalSetRepo.usedBySubset(subsetSuites, partitionNumber, totalPartitions);
    }
}
