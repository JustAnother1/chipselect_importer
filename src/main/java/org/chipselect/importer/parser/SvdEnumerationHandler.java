package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdEnumerationHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;

    public SvdEnumerationHandler(Server srv)
    {
        this.srv = srv;
    }

    public boolean updateEnumeration(Element enumeration, int srvId)
    {
        Response enumRes = srv.get("enumeration", "field_id=" + srvId);
        if(false == enumRes.wasSuccessfull())
        {
            return false;
        }
        // else -> go on
        List<Element> enumList = enumeration.getChildren();
        for(Element enumE : enumList)
        {
            if(false == checkEnumeration(enumRes, enumE))
            {
                return false;
            }
        }
        return true;
    }

    private boolean checkEnumeration(Response enumRes, Element enumE)
    {
        log.error("check enumeration not implemented!");
        return false;
    }

}
