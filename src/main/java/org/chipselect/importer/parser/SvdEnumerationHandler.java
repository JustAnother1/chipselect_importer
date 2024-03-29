package org.chipselect.importer.parser;

import java.util.List;
import java.util.Vector;

import org.chipselect.importer.Tool;
import org.chipselect.importer.server.Request;
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

    public boolean updateEnumeration(Vector<Element> enum_values, int fieldId)
    {
        if(null == enum_values)
        {
            log.error("enum_values is null !");
            return false;
        }
        if(false == enum_values.isEmpty())
        {
            if(0 == fieldId)
            {
                log.error("field ID invalid !");
                return false;
            }
            Request req = new Request("enumeration_element", Request.GET);
            req.addPostParameter("field_id", fieldId);
            Response enumValRes = srv.execute(req);
            if(false == enumValRes.wasSuccessfull())
            {
                log.error("could not read enumeration from server");
                return false;
            }
            log.info("found " + enum_values.size() + " enums in SVD.");
            log.info("found " + enumValRes.numResults() + " enums on the server.");
            for(int i = 0; i < enum_values.size(); i++)
            {
                Element value = enum_values.get(i);
                if(false == checkEnumValues(enumValRes, value, fieldId))
                {
                    log.error("Failed to check enumeration element !");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkEnumValues(Response res, Element enumE, int fieldId)
    {
        String svdName = null;
        String svdDescription = null;
        String svdValue = null;
        boolean svdIsDefault = false;

        // check for unknown children
        List<Element> children = enumE.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            // all defined child types from SVD standard
            // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_registers.html#elem_enumeratedValues

            case "name":
                svdName = Tool.cleanupString(child.getText());
                break;

            case "description":
                svdDescription  = Tool.cleanupString(child.getText());
                break;

            case "value":
                svdValue  = Tool.cleanupString(child.getText());
                break;

            case "isDefault":
                svdIsDefault = Boolean.valueOf(child.getText());
                break;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown enumeration value child tag: {}", name);
                return false;
            }
        }

        boolean found = false;
        boolean changed = false;
        int numEnumValuesOnServer = res.numResults();
        for(int i = 0; i < numEnumValuesOnServer; i++)
        {
            String srvName = res.getString(i, "name");
            if(null == srvName)
            {
                log.warn("Server has unnamed enumeration value !");
                continue;
            }
            if(true == srvName.equals(svdName))
            {
                found = true;
                // description
                if(null == svdDescription)
                {
                    // value not in SVD -> ignore
                }
                else
                {
                    String srvDescription = res.getString(i, "description");
                    if(false == svdDescription.equals(srvDescription))
                    {
                        log.trace("Description changed from {} to {} !", srvDescription, svdDescription);
                        changed = true;
                    }
                }
                // value
                if(null == svdValue)
                {
                    // value not in SVD -> ignore
                }
                else
                {
                    String srvValue = res.getString(i, "value");
                    if(false == svdValue.equals(srvValue))
                    {
                        log.trace("value changed from {} to {} !", srvValue, svdValue);
                        changed = true;
                    }
                }
                // isDefault
                int srvIsDefault = res.getInt(i, "isDefault");
                if(true == svdIsDefault)
                {
                    if(0 == srvIsDefault)
                    {
                        log.trace("isDefault changed from 1 to 0 !");
                        changed = true;
                    }
                }
                else
                {
                    if(1 == srvIsDefault)
                    {
                        log.trace("isDefault changed from 0 to 1 !");
                        changed = true;
                    }
                }

                if(true == changed)
                {
                    // update the enumeration value
                    int valId = res.getInt(i, "id");
                    if(false == updateEnumerationValueOnServer(valId, svdName, svdDescription, svdValue, svdIsDefault))
                    {
                        log.error("Could not update the enumeration value on the server!");
                        return false;
                    }
                }
            }
            // else continue
        }
        if(false == found)
        {
            // create new enumeration for this field
            int valId = createEnumerationValueOnServer(fieldId, svdName, svdDescription, svdValue, svdIsDefault);
            if(0 == valId)
            {
                log.error("could not create a enumeration value !");
                return false;
            }
        }

        return true;
    }

    private int createEnumerationValueOnServer(
            int fieldId,
            String name,
            String description,
            String value,
            boolean isDefault)
    {
        Request req = new Request("enumeration_element", Request.POST);
        req.addPostParameter("field_id", fieldId);
        if(null != name)
        {
            req.addPostParameter("name", name);
        }
        if(null != description)
        {
            req.addPostParameter("description", description);
        }
        if(null != value)
        {
            req.addPostParameter("value", value);
        }
        if(true == isDefault)
        {
            req.addPostParameter("isDefault", 1);
        }
        else
        {
            req.addPostParameter("isDefault", 0);
        }
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not create new enumeration value on server");
            return 0;
        }
        else
        {
            return res.getInt("id");
        }
    }

    private boolean updateEnumerationValueOnServer(
            int valId,
            String name,
            String description,
            String value,
            boolean isDefault)
    {
        Request req = new Request("enumeration_element", Request.PUT);
        req.addPostParameter("id", valId);
        if(null != name)
        {
            req.addPostParameter("name", name);
        }
        if(null != description)
        {
            req.addPostParameter("description", description);
        }
        if(null != value)
        {
            req.addPostParameter("value", value);
        }
        if(true == isDefault)
        {
            req.addPostParameter("isDefault", 1);
        }
        else
        {
            req.addPostParameter("isDefault", 0);
        }
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not update the enumeration volue on the server");
            return false;
        }
        else
        {
            return true;
        }
    }
}
