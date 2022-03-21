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
    private final int[] numRequests = new int[Request.MAX_TYPE_NUM + 1];

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
        for(int i = 0; i < Request.MAX_TYPE_NUM + 1; i++)
        {
            numRequests[i] = 0;
        }
    }

    @Override
    public String getStatus()
    {
        StringBuilder sb = new StringBuilder();
        if(0 < numRequests[Request.GET])
        {
            sb.append("GET : " + numRequests[Request.GET] + " Requests\n");
        }
        if(0 < numRequests[Request.POST])
        {
            sb.append("POST : " + numRequests[Request.POST] + " Requests\n");
        }
        if(0 < numRequests[Request.PUT])
        {
            sb.append("PUT : " + numRequests[Request.PUT] + " Requests\n");
        }
        if(0 < numRequests[Request.PATCH])
        {
            sb.append("PATCH : " + numRequests[Request.PATCH] + " Requests\n");
        }
        if(0 < numRequests[Request.DELETE])
        {
            sb.append("DELETE : " + numRequests[Request.DELETE] + " Requests\n");
        }
        return sb.toString();
    }

    @Override
    public Response execute(Request req)
    {
        Response res = new Response();
        HttpURLConnection connection = null;
        numRequests[req.getType()] ++;
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
            String val = connection.getHeaderField(0);
            log.error("status line: " + val);
            int i = 1;
            val = connection.getHeaderField(i);
            while(null != val)
            {
                log.error(connection.getHeaderFieldKey(i) + " : " + val);
                i++;
                val = connection.getHeaderField(i);
            }
            res.setError(e.toString());
        }
        return res;
    }

}
