package org.chipselect.importer.server;

import java.io.IOException;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RestServer implements Server
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public RestServer()
    {

    }

    protected abstract Response getResponse(Request req) throws IOException;

    private Response handleRequest(int type, String ressource, String urlGet)
    {
        Request req = new Request(ressource);
        req.setType(type);
        req.addGetParameter(urlGet);
        Response res;
        try
        {
            res = getResponse(req);
        }
        catch (IOException e)
        {
            switch(type)
            {
            case Request.GET :
                log.error("GET Request failed!");
                break;

            case Request.POST :
                log.error("POST Request failed!");
                break;

            case Request.PUT :
                log.error("PUT Request failed!");
                break;

            default:
                log.error("Request of type {} failed!", type);
                break;
            }
            log.error("ressource : {}", ressource);
            log.error("urlGet : {}", urlGet);
            log.error(e.toString());
            res = new Response();
            res.setError(e.toString());
        }
        catch (JSONException e1)
        {
            log.error(e1.toString());
            res = new Response();
            res.setError(e1.toString());
        }
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
