package tlb.server.resources.correctness;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.TlbConstants;
import tlb.domain.SuiteNameCountEntry;
import tlb.domain.TimeProvider;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateUniversalSetResourceTest {
    private Context context;
    private Request request;
    private UpdateUniversalSetResource resource;
    private EntryRepoFactory repoFactory;
    private SetRepo repo;
    private Response response;

    @Before
    public void setUp() throws IOException {
        context = new Context();
        request = mock(Request.class);
        repoFactory = mock(EntryRepoFactory.class);
        repo = new SetRepo(new TimeProvider());
        repo.setIdentifier("foo-bar-baz");
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) repoFactory));
        HashMap<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "family_name");
        attributeMap.put(TlbConstants.Server.LISTING_VERSION, "version-string");
        when(request.getAttributes()).thenReturn(attributeMap);
        when(repoFactory.createUniversalSetRepo("family_name", "version-string")).thenReturn(repo);
        response = mock(Response.class);
        resource = new UpdateUniversalSetResource(context, request, response);
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
    public void shouldCreateUniversalSetRepo_whenGivenTheListingByFirstPartition() throws ResourceException, IOException {
        resource.acceptRepresentation(new StringRepresentation("foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class"));
        assertThat(repo.list().size(), is(3));
        assertThat(repo.list(), hasItems(new SuiteNameCountEntry("foo.bar.Baz.class"), new SuiteNameCountEntry("bar.baz.Bang.class"), new SuiteNameCountEntry("baz.bang.Quux.class")));
        verify(response).setStatus(Status.SUCCESS_CREATED);
    }

    @Test
    public void shouldMatchUniversalSetRepo_whenGivenTheListingByAnyPartitionAfterFirst() throws ResourceException, IOException {
        String suites = "foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class";
        repo.load(suites);
        List<SuiteNameCountEntry> listBefore2ndPartitionPosting = new ArrayList<SuiteNameCountEntry>(repo.list());

        resource.acceptRepresentation(new StringRepresentation(suites));

        assertThat(repo.list().size(), is(3));
        assertThat(repo.list(), hasItems(new SuiteNameCountEntry("foo.bar.Baz.class"), new SuiteNameCountEntry("bar.baz.Bang.class"), new SuiteNameCountEntry("baz.bang.Quux.class")));

        List<SuiteNameCountEntry> listAfter2ndPartitionPosting = new ArrayList<SuiteNameCountEntry>(repo.list());

        Collections.sort(listBefore2ndPartitionPosting, new SuiteNameCountEntryComparator());
        Collections.sort(listAfter2ndPartitionPosting, new SuiteNameCountEntryComparator());
        for (int i = 0; i < listBefore2ndPartitionPosting.size(); i++) {
             assertThat(listBefore2ndPartitionPosting.get(i), sameInstance(listAfter2ndPartitionPosting.get(i)));
        }

        verify(response).setStatus(Status.SUCCESS_OK);
    }

    private static class SuiteNameCountEntryComparator implements Comparator<SuiteNameCountEntry> {
        public int compare(SuiteNameCountEntry o1, SuiteNameCountEntry o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
