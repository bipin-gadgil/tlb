package tlb.ant;

import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Test;
import tlb.service.Server;
import tlb.splitter.correctness.ValidationResult;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class CheckMissingPartitionsTest {

    private CheckMissingPartitions task;
    private Server server;

    @Before
    public void setUp() throws Exception {
        Map<String, String> envMap = new HashMap<String, String>();
        SystemEnvironment env = new SystemEnvironment(envMap);
        server = mock(Server.class);
        task = new CheckMissingPartitions(env, server);
    }

    @Test
    public void shouldQueryTlbServerForModulesPartitionCompleteness() {
        when(server.verifyAllPartitionsExecutedFor("foo")).thenReturn(new ValidationResult(ValidationResult.Status.OK, ""));
        when(server.verifyAllPartitionsExecutedFor("bar")).thenReturn(new ValidationResult(ValidationResult.Status.OK, ""));
        task.setModuleNames("foo, bar");
        task.execute();
        verify(server).verifyAllPartitionsExecutedFor("foo");
        verify(server).verifyAllPartitionsExecutedFor("bar");
        verifyNoMoreInteractions(server);
    }
    
    @Test
    public void shouldFailTheTaskIfAllPartitionsHaveNotExecutedForAModule() {
        when(server.verifyAllPartitionsExecutedFor("foo")).thenReturn(new ValidationResult(ValidationResult.Status.OK, ""));
        when(server.verifyAllPartitionsExecutedFor("bar")).thenReturn(new ValidationResult(ValidationResult.Status.FAILED, "Partition(s) [2/3] didn't execute for module 'bar' in version 'world' of job-name 'hello'."));
        task.setModuleNames("foo, bar");
        String exceptionMessage = null;
        try {
            task.execute();
            fail("should not finish validation check successfully when one of the partitions has not run.");
        } catch (BuildException e) {
            exceptionMessage = e.getMessage();
        }
        verify(server).verifyAllPartitionsExecutedFor("foo");
        verify(server).verifyAllPartitionsExecutedFor("bar");
        assertThat(exceptionMessage, is("Partition(s) [2/3] didn't execute for module 'bar' in version 'world' of job-name 'hello'.\n\n"));
        verifyNoMoreInteractions(server);
    }

}
