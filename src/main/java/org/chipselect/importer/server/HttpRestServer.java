package org.chipselect.importer.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

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

    protected Response getResponse(Request req) throws IOException
    {
        // Create a neat value object to hold the URL
        URL url = new URL(restUrl + req.url());
        log.info("{} : {}",req.getMethod(), url.toString());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(req.getMethod());
        connection.setRequestProperty("accept", "application/json");
        if(true == hasUser)
        {
            Base64.Encoder enc = Base64.getUrlEncoder();
            connection.addRequestProperty("Authorization", "Basic " + enc.encodeToString((restUser + ":" + restPassword).getBytes()));
        }
        connection.connect();
        InputStream responseStream = connection.getInputStream();

        Response res = new Response();
        res.readFrom(responseStream);
        return res;
    }


}
