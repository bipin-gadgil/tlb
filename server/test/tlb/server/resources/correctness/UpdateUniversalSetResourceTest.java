package tlb.server.resources.correctness;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import tlb.TlbConstants;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;

import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateUniversalSetResourceTest {
    private Context context;
    private Request request;
    private UpdateUniversalSetResource resource;

    @Before
    public void setUp() {
        context = new Context();
        request = mock(Request.class);
        EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        SetRepo repo = mock(SetRepo.class);
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) repoFactory));
        Request request = mock(Request.class);
        HashMap<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "family_name");
        when(request.getAttributes()).thenReturn(attributeMap);
        when(repoFactory.createUniversalSetRepo(TlbConstants.Server.REQUEST_NAMESPACE, "version-string")).thenReturn(repo);
        resource = new UpdateUniversalSetResource(context, request, mock(Response.class));
    }

    @Test
    public void shouldAllowPost() {
        assertThat(resource.allowPost(), is(true));
    }
}
