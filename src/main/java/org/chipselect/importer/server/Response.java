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

    public Response()
    {
    }

    public int getInt(String key)
    {
        if(null == dataArr)
        {
            return 0;
        }
        else
        {
            // assume we only have one element
            try
            {
                JSONObject obj = dataArr.getJSONObject(0);
                return obj.getInt(key);
            }
            catch(JSONException e)
            {
                return 0;
            }
        }
    }

    public void readFrom(InputStream responseStream) throws IOException
    {
        String JsonString = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        dataArr = new JSONArray(JsonString);
        log.trace("{}", dataArr.toString());
    }

}
