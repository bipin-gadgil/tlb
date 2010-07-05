package com.github.tlb.server.resources;

import com.github.tlb.server.repo.EntryRepo;
import com.github.tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import java.io.IOException;

/**
 * @understands result of suite reported by job
 */
public class SuiteResultResource extends TlbResource {

    public SuiteResultResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected EntryRepo getRepo(EntryRepoFactory repoFactory, String key) throws IOException, ClassNotFoundException {
        return repoFactory.createSuiteResultRepo(key, EntryRepoFactory.LATEST_VERSION);
    }

    @Override
    public boolean allowPut() {
        return true;
    }
}