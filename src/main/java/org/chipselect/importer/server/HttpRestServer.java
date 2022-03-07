package org.chipselect.importer.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRestServer extends RestServer implements Server
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final String restUrl;
    private final String restUser;
    private final String restPassword;
    private final boolean hasUser;

    public HttpRestServer(String restUrl, String restUser, String restPassword)
    {
        super();
        this.restUrl = restUrl;
        this.restUser = restUser;
        this.restPassword = restPassword;
        if((null != restUser) && (null != restPassword))
        {
            hasUser = true;
        }
        else
        {
            hasUser = false;
        }
    }

    protected Response getResponse(Request req)
    {
        Response res = new Response();
        HttpURLConnection connection = null;
        // Create a neat value object to hold the URL
        try
        {
            URL url = new URL(restUrl + req.url());
            log.info("{} : {}",req.getMethod(), url.toString());

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(req.getMethod());
            connection.setRequestProperty("accept", "application/json");
            if(true == hasUser)
            {
                Base64.Encoder enc = Base64.getUrlEncoder();
                connection.addRequestProperty("Authorization", "Basic " + enc.encodeToString((restUser + ":" + restPassword).getBytes()));
            }
            connection.connect();
            InputStream responseStream = connection.getInputStream();

            res.readFrom(responseStream);
        }
        catch (JSONException e)
        {
            log.error("{} Request failed! JSONException!", req.getMethod());
            log.error("url : {}", req.url());
            log.error(e.toString());
            log.error("Failure Description : {}", res.getFailureDescription());
            res.setError(e.toString());
        }
        catch (MalformedURLException e)
        {
            log.error("{} Request failed! MalformedURLException!", req.getMethod());
            log.error("url : {}", req.url());
            log.error(e.toString());
            res.setError(e.toString());
        }
        catch (ProtocolException e)
        {
            log.error("{} Request failed! ProtocolException!", req.getMethod());
            log.error("url : {}", req.url());
            log.error(e.toString());
            res.setError(e.toString());
        }
        catch (IOException e)
        {
            log.error("{} Request failed! IOException!", req.getMethod());
            log.error("url : {}", req.url());
            log.error(e.toString());
            log.error("X-debug header: {}", connection.getHeaderField("X-debug"));
            log.error("X-EXCEPTION header: {}", connection.getHeaderField("X-EXCEPTION"));
            log.error("X-SQL header: {}", connection.getHeaderField("X-SQL"));
            res.setError(e.toString());
        }
        return res;
    }


}
