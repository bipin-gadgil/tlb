package tlb.twist;

import org.apache.tools.ant.*;

import tlb.utils.SystemEnvironment;
import tlb.factory.TlbFactory;
import static tlb.TlbConstants.TLB_SPLITTER;

public class FilterScenariosTask extends Task {

    public static final String DEFAULT_TWIST_LOCATION = "tlb-balanced-filtered-twist-scenarios";

    private final LoadBalancedTwistSuite suite;
    private String scenariosFolder;
    private String destinationFolder = DEFAULT_TWIST_LOCATION;

    //Needed for ant
    public FilterScenariosTask() {
        this(new SystemEnvironment());
    }

    private FilterScenariosTask(SystemEnvironment systemEnvironment) {
        this(new LoadBalancedTwistSuite(TlbFactory.getCriteria(systemEnvironment.val(TLB_SPLITTER), systemEnvironment)));
    }
    
    FilterScenariosTask(LoadBalancedTwistSuite suite) {
        this.suite = suite;
    }

    @Override
    public void execute() throws BuildException {
        suite.balance(scenariosFolder, destinationFolder);
    }

    @Override
    public String getTaskName() {
        return "filterScenarios";
    }

    public void setScenariosFolder(String scenariosFolder) {
        this.scenariosFolder = scenariosFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }
}
