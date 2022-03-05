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
    private String JsonString = null;

    public Response()
    {
    }

    public boolean wasSuccessfull()
    {
        return success;
    }

    public String getFailureDescription()
    {
        return ErrorMessage + "(Response: " + JsonString + ")\n";
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
        JsonString = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
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
            JSONObject obj = dataArr.getJSONObject(index);
            if(false == obj.has(key))
            {
                return 0;
            }
            if(true == obj.isNull(key))
            {
                return 0;
            }
            try
            {
                return obj.getInt(key); // does not work with hex numbers like "0x400"
            }
            catch(JSONException e)
            {
                // might just be a hex number
                try
                {
                    String hlp = obj.getString(key);
                    int res = Integer.decode(hlp);
                    return res;
                }
                catch(JSONException e2)
                {
                    log.warn("JSON: Int convert Exception !(key: {} - {})", key, obj);
                    return 0;
                }
                catch(NumberFormatException e1)
                {
                    log.warn("Int convert Exception !(key: {} - {})", key, obj);
                    return 0;
                }
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
                String res = obj.getString(key);
                res = res.trim();
                return res;
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
