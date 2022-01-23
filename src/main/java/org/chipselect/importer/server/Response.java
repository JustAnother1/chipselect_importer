package org.chipselect.importer.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Response
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private JSONArray dataArr = null;
    private boolean success = true;
    private String ErrorMessage = null;

    public Response()
    {
    }

    public boolean wasSuccessfull()
    {
        return success;
    }

    public String getFailureDescription()
    {
        return ErrorMessage;
    }

    public int numResults()
    {
        if(null == dataArr)
        {
            return 0;
        }
        else
        {
            return dataArr.length();
        }
    }

    public void readFrom(InputStream responseStream) throws IOException
    {
        String JsonString = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        dataArr = new JSONArray(JsonString);
        log.trace("{}", dataArr.toString());
    }

    public int getInt(String key)
    {
        return getInt(0, key);
    }

    public int getInt(int index, String key)
    {
        if(null == dataArr)
        {
            return 0;
        }
        else
        {
            try
            {
                JSONObject obj = dataArr.getJSONObject(index);
                return obj.getInt(key);
            }
            catch(JSONException e)
            {
                return 0;
            }
        }
    }

    public String getString(String key)
    {
        return getString(0, key);
    }

    public String getString(int index, String key)
    {
        if(null == dataArr)
        {
            return "";
        }
        else
        {
            try
            {
                JSONObject obj = dataArr.getJSONObject(index);
                return obj.getString(key);
            }
            catch(JSONException e)
            {
                return "";
            }
        }
    }

    public void setError(String msg)
    {
        success = false;
        ErrorMessage = msg;
    }

    public boolean getBoolean(int index, String key)
    {
        if(null == dataArr)
        {
            return false;
        }
        else
        {
            try
            {
                JSONObject obj = dataArr.getJSONObject(index);
                return obj.getBoolean(key);
            }
            catch(JSONException e)
            {
                return false;
            }
        }
    }

}
