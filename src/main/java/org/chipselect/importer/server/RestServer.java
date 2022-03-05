package org.chipselect.importer.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RestServer implements Server
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public RestServer()
    {

    }

    protected abstract Response getResponse(Request req);

    private Response handleRequest(int type, String ressource, String urlGet)
    {
        Request req = new Request(ressource);
        req.setType(type);
        req.addGetParameter(urlGet);
        Response res;
        res = getResponse(req);
        return res;
    }

    @Override
    public Response get(String ressource, String urlGet)
    {
        return handleRequest(Request.GET, ressource, urlGet);
    }

    @Override
    public Response post(String ressource, String urlGet)
    {
        return handleRequest(Request.POST, ressource, urlGet);
    }

    @Override
    public Response put(String ressource, String urlGet)
    {
        return handleRequest(Request.PUT, ressource, urlGet);
    }

}
