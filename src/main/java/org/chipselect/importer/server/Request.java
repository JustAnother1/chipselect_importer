package org.chipselect.importer.server;

import java.util.Vector;

public class Request
{
    public static final int GET = 1;
    public static final int POST = 2;
    public static final int PUT = 3;
    public static final int PATCH = 4;
    public static final int DELETE = 5;

    private final String resource;
    private int type;
    private Vector<String> urlGet = new Vector<String>();

    public Request(String resource)
    {
        this.resource = resource;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public void addGetParameter(String filter)
    {
        urlGet.add(filter);
    }

    public String url()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(resource);
        if(0 < urlGet.size())
        {
            sb.append("?");
            sb.append(urlGet.elementAt(0));
            for(int i = 1; i < urlGet.size(); i++)
            {
                sb.append("&" + urlGet.elementAt(i));
            }
        }
        // else no GET variables
        return sb.toString();
    }

    public String getMethod()
    {
        switch(type)
        {
        case GET:    return "GET";
        case POST:   return "POST";
        case PUT:    return "PUT";
        case PATCH:  return "PATCH";
        case DELETE: return "DELETE";
        }
        return "GET";
    }

}