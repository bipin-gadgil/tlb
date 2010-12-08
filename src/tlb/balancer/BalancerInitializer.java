package tlb.balancer;

import tlb.TlbConstants;
import tlb.factory.TlbFactory;
import tlb.server.ServerInitializer;
import tlb.utils.SystemEnvironment;
import org.restlet.Context;
import org.restlet.Restlet;

import java.util.HashMap;

/**
 * @understands initializing Balancer tlb restlet app
 */
public class BalancerInitializer extends ServerInitializer {
    private final SystemEnvironment env;

    public BalancerInitializer(SystemEnvironment env) {
        this.env = env;
    }

    @Override
    protected int appPort() {
        return Integer.parseInt(env.val(TlbConstants.Balancer.TLB_BALANCER_PORT));
    }

    @Override
    public Restlet application() {
        HashMap<String, Object> appMap = new HashMap<String, Object>();
        appMap.put(TlbClient.SPLITTER, TlbFactory.getCriteria(env.val(TlbConstants.TLB_CRITERIA), env));
        appMap.put(TlbClient.ORDERER, TlbFactory.getOrderer(env.val(TlbConstants.TLB_ORDERER), env));
        appMap.put(TlbClient.TALK_TO_SERVICE, TlbFactory.getTalkToService(env));
        appMap.put(TlbClient.APP_COMPONENT, init());
        Context applicationContext = new Context();
        applicationContext.setAttributes(appMap);
        return new TlbClient(applicationContext);
    }
}