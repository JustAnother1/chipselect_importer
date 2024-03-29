package org.chipselect.importer.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.chipselect.importer.Tool;
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

    @Override
    public String toString()
    {
        if(true == success)
        {
            return "successful Response: " + JsonString + " -> " + dataArr.toString() + ")\n";
        }
        else
        {
            return "failed Response: " + ErrorMessage + "(Response: " + JsonString + ")\n";
        }
    }

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
        log.trace("{}", JsonString);
        // log.trace("{}", dataArr.toString());
    }

    public int getInt(String key)
    {
        return getInt(0, key);
    }

    public int getInt(int index, String key)
    {
        if(null == dataArr)
        {
            log.warn("No data array to read from");
            return 0;
        }
        else
        {
            if(0 ==  dataArr.length())
            {
                log.warn("data array is empty");
                return 0;
            }
            JSONObject obj = dataArr.getJSONObject(index);
            if(false == obj.has(key))
            {
                log.warn("requested key not in data entry");
                return 0;
            }
            if(true == obj.isNull(key))
            {
                // log.warn("requested key({}) is null", key);
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
                    int res = (int)Tool.decode(hlp);
                    return res;
                }
                catch(JSONException e2)
                {
                    log.warn("JSON: Int convert Exception !(key: {} - {})", key, obj);
                    System.exit(98);
                    return 0;
                }
                catch(NumberFormatException e1)
                {
                    log.warn("Int convert Exception !(key: {} - {})", key, obj);
                    System.exit(99);
                    return 0;
                }
            }
        }
    }

    public long getLong(String key)
    {
        return getLong(0, key);
    }

    public long getLong(int index, String key)
    {
        if(null == dataArr)
        {
            log.warn("No data array to read from");
            return 0;
        }
        else
        {
            if(0 ==  dataArr.length())
            {
                log.warn("data array is empty");
                return 0;
            }
            JSONObject obj = dataArr.getJSONObject(index);
            if(false == obj.has(key))
            {
                log.warn("requested key not in data entry");
                return 0;
            }
            if(true == obj.isNull(key))
            {
                // log.warn("requested key({}) is null", key);
                return 0;
            }
            try
            {
                return obj.getLong(key); // does not work with hex numbers like "0x400"
            }
            catch(JSONException e)
            {
                // might just be a hex number
                try
                {
                    String hlp = obj.getString(key);
                    long res = Tool.decode(hlp);
                    return res;
                }
                catch(JSONException e2)
                {
                    log.warn("JSON: Int convert Exception !(key: {} - {})", key, obj);
                    System.exit(100);
                    return 0;
                }
                catch(NumberFormatException e1)
                {
                    log.warn("Int convert Exception !(key: {} - {})", key, obj);
                    System.exit(101);
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
            if(0 ==  dataArr.length())
            {
                log.warn("data array is empty");
                return "";
            }
            JSONObject obj = dataArr.getJSONObject(index);
            if(false == obj.has(key))
            {
                log.warn("requested key not in data entry");
                return "";
            }
            if(true == obj.isNull(key))
            {
                // log.warn("requested key({}) is null", key);
                return "";
            }
            try
            {
                String res = JSONObject.valueToString(obj.get(key));
                res = res.trim();
                if(true == res.matches("\".*\""))
                {
                    res = res.substring(1, res.length() -1);
                }
                // String res = obj.getString(key); // this does not work on int values !
                return res;
            }
            catch(JSONException e)
            {
                System.exit(102);
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
            if(0 ==  dataArr.length())
            {
                log.warn("data array is empty");
                return false;
            }
            JSONObject obj = dataArr.getJSONObject(index);
            if(false == obj.has(key))
            {
                log.warn("requested key not in data entry");
                return false;
            }
            if(true == obj.isNull(key))
            {
                // log.warn("requested key({}) is null", key);
                return false;
            }
            try
            {
                return obj.getBoolean(key);
            }
            catch(JSONException e)
            {
                System.exit(103);
                return false;
            }
        }
    }

    public String dump(int index)
    {
        JSONObject obj = dataArr.getJSONObject(index);
        if(null == obj)
        {
            return "";
        }
        return obj.toString();
    }

    public String dumpAllNames()
    {
        if(null == dataArr)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < numResults(); i++)
        {
            sb.append(getString(i, "name"));
            sb.append(", ");
        }
        return sb.toString();
    }

}
