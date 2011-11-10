package tlb.server.resources.correctness;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.TlbConstants;
import tlb.domain.SuiteNameCountEntry;
import tlb.domain.TimeProvider;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class UpdateSubsetResourceTest {
    private Context context;
    private Request request;
    private UpdateSubsetResource resource;
    private EntryRepoFactory repoFactory;
    private SetRepo repo;
    private Response response;
    private Representation representationGiven;
    private HashMap<String, Object> reqAttrMap;

    @Before
    public void setUp() throws IOException {
        context = new Context();
        request = mock(Request.class);
        repoFactory = mock(EntryRepoFactory.class);
        repo = new SetRepo(new TimeProvider());
        repo.setIdentifier("foo-bar-baz");
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) repoFactory));
        reqAttrMap = new HashMap<String, Object>();
        reqAttrMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "family_name");
        reqAttrMap.put(TlbConstants.Server.LISTING_VERSION, "version-string");
        reqAttrMap.put(TlbConstants.Server.MODULE_NAME, "my-module");
        when(request.getAttributes()).thenReturn(reqAttrMap);
        when(repoFactory.createUniversalSetRepo("family_name", "version-string", "my-module")).thenReturn(repo);
        response = mock(Response.class);

        representationGiven = null;

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                representationGiven = (Representation) invocationOnMock.getArguments()[0];
                return null;
            }
        }).when(response).setEntity(any(Representation.class));

        resource = new UpdateSubsetResource(context, request, response);
    }

    @Test
    public void shouldAllow_ONLY_Post() {
        assertThat(resource.allowPost(), is(true));
        assertThat(resource.allowPut(), is(false));
        assertThat(resource.allowGet(), is(false));
        assertThat(resource.allowHead(), is(false));
        assertThat(resource.allowDelete(), is(false));
    }

    @Test
    public void shouldFailSubsetCheck_whenNoUniversalSetGiven() throws ResourceException, IOException {
        resource.acceptRepresentation(new StringRepresentation("foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class"));
        assertThat(repo.list().size(), is(0));
        verify(response).setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
        assertThat(representationGiven.getText(), is("Universal set for given job-name, job-version and module-name combination doesn't exist."));
    }

    @Test
    public void shouldUpdateUniversalSetRepoSuiteCounts_forSuitesConsumedByASubset() throws ResourceException, IOException {
        repo.load("foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class");
        List<SuiteNameCountEntry> listBefore = new ArrayList<SuiteNameCountEntry>(repo.list());
        resource.acceptRepresentation(new StringRepresentation("foo.bar.Baz.class\nbaz.bang.Quux.class"));
        List<SuiteNameCountEntry> listAfter = new ArrayList<SuiteNameCountEntry>(repo.list());
        assertThat(listAfter.size(), is(3));
        for (int i = 0; i < listBefore.size(); i++) {
            assertThat(listBefore.get(i), sameInstance(listAfter.get(i)));
        }
        assertThat(listAfter.get(0).getCount(), is((short) 0));
        assertThat(listAfter.get(1).getCount(), is((short) 1));
        assertThat(listAfter.get(2).getCount(), is((short) 0));

        verify(response).setStatus(Status.SUCCESS_OK);
        assertThat(representationGiven, is(nullValue()));
    }
}
