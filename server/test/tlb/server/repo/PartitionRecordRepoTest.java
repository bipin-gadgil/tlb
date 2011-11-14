package tlb.server.repo;

import org.junit.Before;
import org.junit.Test;
import tlb.domain.PartitionIdentifier;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PartitionRecordRepoTest {

    private PartitionRecordRepo repo;
    private SetRepo.OperationResult operationResult;

    @Before
    public void setUp() throws Exception {
        repo = new PartitionRecordRepo();
        operationResult = new SetRepo.OperationResult(true);
    }

    @Test
    public void shouldUnderstandWhen_allPartitionsHaveReportedSubsetValues_inSimpleScenario() {
        repo.subsetReceivedFromPartition(new PartitionIdentifier(1, 3));
        assertThat(repo.allSubsetsReceived(operationResult), is(false));
        assertThat(operationResult.isSuccess(), is(true));

        repo.subsetReceivedFromPartition(new PartitionIdentifier(3, 3));
        assertThat(repo.allSubsetsReceived(operationResult), is(false));
        assertThat(operationResult.isSuccess(), is(true));

        repo.subsetReceivedFromPartition(new PartitionIdentifier(2, 3));
        assertThat(repo.allSubsetsReceived(operationResult), is(true));
        assertThat(operationResult.isSuccess(), is(true));
    }

    @Test
    public void shouldIdentifyWhenHasPartitionsRunningWithInconsistentTotalPartitionsConfiguration() {
        repo.subsetReceivedFromPartition(new PartitionIdentifier(1, 3));
        assertThat(repo.allSubsetsReceived(operationResult), is(false));
        assertThat(operationResult.isSuccess(), is(true));

        repo.subsetReceivedFromPartition(new PartitionIdentifier(3, 4));
        assertThat(repo.allSubsetsReceived(operationResult), is(false));
        assertThat(operationResult.isSuccess(), is(false));
        assertThat(operationResult.getMessage(), is("- Partitions [1/3, 3/4] are being run with inconsistent total-partitions configuration. This may lead to violation of mutual-exclusion or collective-exhaustion or both. Total partitions value should be the same across all partitions of a job-name and job-version combination.\n"));

        repo.subsetReceivedFromPartition(new PartitionIdentifier(2, 3));
        assertThat(repo.allSubsetsReceived(operationResult), is(true));
        assertThat(operationResult.isSuccess(), is(false));
    }

    @Test
    public void shouldParsePartitionRecord() {
        List<PartitionIdentifier> parsed = repo.parse("2/3\n3/3\n1/3");
        assertThat(parsed.get(0), is(new PartitionIdentifier(2, 3)));
        assertThat(parsed.get(1), is(new PartitionIdentifier(3, 3)));
        assertThat(parsed.get(2), is(new PartitionIdentifier(1, 3)));
    }
}
