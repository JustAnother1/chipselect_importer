package org.chipselect.importer.server;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class Request
{
    public static final int GET = 1;
    public static final int POST = 2;
    public static final int PUT = 3;
    public static final int PATCH = 4;
    public static final int DELETE = 5;
    public static final int MAX_TYPE_NUM = 5;

    private final String resource;
    private final int type;
    private Vector<String> urlPost = new Vector<String>();

    public Request(String resource, int type)
    {
        this.resource = resource;
        this.type = type;
        urlPost.add("REQUEST_METHOD=" + getMethodName(type));
    }

    public int getType()
    {
        return type;
    }

    public String url()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(resource);
        return sb.toString();
    }

    public String getMethod()
    {
        return getMethodName(type);
    }

    public static String getMethodName(int type)
    {
        switch(type)
        {
        case GET:    return "GET";
        case POST:   return "POST";
        case PUT:    return "PUT";
        case PATCH:  return "PATCH";
        case DELETE: return "DELETE";
        default:     return "INVALID";
        }
    }

    public boolean hasBody()
    {
        if(0 == urlPost.size())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public byte[] getBodyDataBytes()
    {
    	if(0 == urlPost.size())
    	{
    		return null;
    	}
    	
        StringBuilder sb = new StringBuilder();
    	if(1 == urlPost.size())
    	{
    		sb.append(urlPost.elementAt(0));
    	}
    	else
    	{
    		sb.append(urlPost.elementAt(0));
	        for(int i = 1; i < urlPost.size(); i++)
	        {
	            sb.append("&" + urlPost.elementAt(i));
	        }
    	}

        String data = sb.toString();
        byte[] out = data.getBytes(StandardCharsets.UTF_8);
        return out;
    }

    public void addPostParameter(String variable, String value)
    {
        if((null != variable) && (null != value))
        {
            variable = URLEncoder.encode(variable, StandardCharsets.UTF_8);
            value = URLEncoder.encode(value, StandardCharsets.UTF_8);
            String filter = variable + "=" + value;
            urlPost.add(filter);
        }
    }

    public void addPostParameter(String variable, int value)
    {
        variable = URLEncoder.encode(variable, StandardCharsets.UTF_8);
        String filter = variable + "=" + value;
        urlPost.add(filter);
    }

    public void addPostParameter(String variable, long value)
    {
        variable = URLEncoder.encode(variable, StandardCharsets.UTF_8);
        String filter = variable + "=" + value;
        urlPost.add(filter);
    }

}
