package org.chipselect.importer.server;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private final long[] RequestsTimes = new long[Request.MAX_TYPE_NUM + 1];
    private final long[] RequestsTimeMin = new long[Request.MAX_TYPE_NUM + 1];
    private final long[] RequestsTimeMax = new long[Request.MAX_TYPE_NUM + 1];
    private boolean dryRunMode = false;
    private FileWriter fw = null;

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
            RequestsTimes[i] = 0;
            RequestsTimeMin[i] = Long.MAX_VALUE;
            RequestsTimeMax[i] = 0;
        }
    }

    private String statusForType(int type)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Request.getMethodName(type) + " : " + numRequests[type] + " Requests");
        sb.append(String.format(" (%d/%d/%d)\n",
                RequestsTimeMin[type]/1000000,
                (RequestsTimes[type]/numRequests[type])/1000000,
                RequestsTimeMax[type]/1000000 ) );
        return sb.toString();
    }

    @Override
    public String getStatus()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Type : (min / avareage / max)\n");
        if(true == dryRunMode)
        {
            RequestsTimeMin[Request.DELETE] = 0;
            RequestsTimeMin[Request.PATCH] = 0;
            RequestsTimeMin[Request.POST] = 0;
            RequestsTimeMin[Request.PUT] = 0;
        }
        for(int i = 1; i <= Request.MAX_TYPE_NUM; i++)
        {
            if(0 < numRequests[i])
            {
                sb.append(statusForType(i));
            }
        }
        return sb.toString();
    }

    @Override
    public Response execute(Request req)
    {
        long start = System.nanoTime();
        int reqType = req.getType();
        Response res = new Response();
        HttpURLConnection connection = null;
        numRequests[reqType] ++;
        // Create a neat value object to hold the URL
        if(true == dryRunMode)
        {
            // in dry run we do only GET requests
            if(Request.GET != reqType)
            {
                try
                {
                    // log that request
                    fw.write(req.toString() + "\n");
                    // if we tried to create a new entry we read back the new id
                    InputStream stream = new ByteArrayInputStream("[{id : 1}]".getBytes(StandardCharsets.UTF_8));
                    res.readFrom(stream);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                return res;
            }
        }
        try
        {
            URI uri = new URI(restUrl + req.url());
            URL url = uri.toURL();
            log.trace("{} : {}",req.getMethod(), url.toString());

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(req.getMethod());
            if(true == req.hasBody())
            {
                connection.setDoOutput(true);
            }
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("accept", "application/json");
            if(true == hasUser)
            {
                Base64.Encoder enc = Base64.getUrlEncoder();
                connection.addRequestProperty("Authorization", "Basic " + enc.encodeToString((restUser + ":" + restPassword).getBytes()));
            }
            connection.setRequestMethod("POST");
            if(true == req.hasBody())
            {
                OutputStream requestStream = connection.getOutputStream();
                requestStream.write(req.getBodyDataBytes());
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
        catch (URISyntaxException e)
        {
            log.error("{} Request failed! URISyntaxException!", req.getMethod());
            log.error("url : {}", req.url());
            log.error(e.toString());
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
            log.error("request : {}", req.toString());
            // log.error("url : {}", req.url());
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
        long finish = System.nanoTime();
        long timeElapsed = finish - start;
        RequestsTimes[reqType] += timeElapsed;
        if(timeElapsed > RequestsTimeMax[reqType])
        {
            RequestsTimeMax[reqType] = timeElapsed;
        }
        if(timeElapsed < RequestsTimeMin[reqType])
        {
            RequestsTimeMin[reqType] = timeElapsed;
        }
        return res;
    }

    @Override
    public void enableDryRunMode()
    {
        dryRunMode = true;
        try
        {
            fw = new FileWriter("log_dry_mode.txt");
        }
        catch (IOException e)
        {
            fw = null;
            e.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        if(null != fw)
        {
            try
            {
                fw.flush();
                fw.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

}
