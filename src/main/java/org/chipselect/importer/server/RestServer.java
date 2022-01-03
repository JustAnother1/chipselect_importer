package org.chipselect.importer.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RestServer implements Server
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public RestServer()
    {

    }

    @Override
    public Response get(String ressource, String field, String filter)
    {

        return new Response();
    }

    protected abstract Response getResponse(Request req) throws IOException;

    @Override
    public Response get(String ressource, String filter)
    {
        Request req = new Request(ressource);
        req.setType(Request.GET);
        req.addGetParameter(filter);
        try
        {
            return getResponse(req);
        }
        catch (IOException e)
        {
            log.error(e.toString());
            // log.error(e.getLocalizedMessage());
            // e.printStackTrace();
        }
        return new Response();
    }

    @Override
    public Response post(String ressource, String value)
    {
        Request req = new Request(ressource);
        req.setType(Request.POST);
        req.addGetParameter(value);
        try
        {
            return getResponse(req);
        }
        catch (IOException e)
        {
            log.error(e.toString());
        }
        return new Response();
    }

}
