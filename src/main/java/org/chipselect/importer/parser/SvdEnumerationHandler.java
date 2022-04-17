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

    public boolean updateEnumeration(Element enumeration, int fieldId)
    {
        if(0 == fieldId)
        {
            log.error("Field ID invalid !");
            return false;
        }
        Request req = new Request("enumeration", Request.GET);
        req.addGetParameter("field_id", fieldId);
        Response enumRes = srv.execute(req);
        if(false == enumRes.wasSuccessfull())
        {
            log.error("could not read enumeration from server");
            return false;
        }
        // else -> go on

        if(false == checkEnumeration(enumRes, enumeration, fieldId))
        {
            return false;
        }
        return true;
    }

    private boolean checkEnumeration(Response res, Element enumE, int fieldId)
    {
        String svdName = null;
        String svdUsage  = null;
        Vector<Element> enum_values = new Vector<Element>();

        if(null != enumE.getAttribute("derivedFrom "))
        {
            log.error("Derived enumeration not yet supported!");
            return false;
        }

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

            case "usage":
                svdUsage  = Tool.cleanupString(child.getText());
                break;

            case "headerEnumName":
                log.error("enumeration child headerEnumName(={}) not implemented!", child.getText());
                return false;

            case "enumeratedValue":
                enum_values.add(child);
                break;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown enumeration child tag: {}", name);
                return false;
            }
        }

        int enumId = 0;
        int numEnumsOnServer = res.numResults();
        // there are only two _valid_ cases here:
        // 0 elements means this enum is new and we need to create it, or
        // 1 elements means we already have this enum and only want to update it.
        // everything else is just a big mistake!
        if(0 == numEnumsOnServer)
        {
            // create new enumeration for this field
            enumId = createEnumerationOnServer(fieldId, svdName, svdUsage);
            if(0 == enumId)
            {
                log.error("could not create a enumeration !");
                log.error(Tool.getXMLRepresentationFor(enumE));
                return false;
            }
            // else  OK
        }
        else if(1 == numEnumsOnServer)
        {
            enumId = res.getInt("id");
            String srvName = res.getString("name");
            String srvUsage = res.getString("usage_right");
            boolean changed = false;

            if(null != srvName)
            {
                if(null != svdName)
                {
                    if(false == srvName.equals(svdName))
                    {
                        // this server enumeration has a name and it is different to the name in the SVD
                        log.trace("name changed from {} to {}", srvName, svdName);
                        changed = true;
                    }
                }
                // updated to NULL -> ignore
            }
            else
            {
                if(null != svdName)
                {
                    // this server enum has no name, but the svd one has a name
                    log.trace("name changed from {} to {}", srvName, svdName);
                    changed = true;
                }
            }

            if(null != srvUsage)
            {
                // server does have a usage
                if(null != svdUsage)
                {
                    if(false == svdUsage.equals(srvUsage))
                    {
                        log.trace("usage changed from {} to {}", srvUsage, svdUsage);
                        changed = true;
                    }
                }
            }
            else
            {
                // server has no usage
                if(null == svdUsage)
                {
                    // not changed
                }
                else
                {
                    log.trace("usage changed from {} to {}", srvUsage, svdUsage);
                    changed = true;
                }
            }
            if(true == changed)
            {
                // update the enumeration
                if(false == updateEnumerationOnServer(enumId, svdName, svdUsage))
                {
                    log.error("Could not update the enumeration on the server!");
                    return false;
                }
            }
        }
        else
        {
            // -> more than one enum? -> Server has invalid data!
            log.error("server has more than one enumeration for a single field !");
            return false;
        }

        if(false == enum_values.isEmpty())
        {
            if(0 == enumId)
            {
                log.error("enumeration ID invalid !");
                return false;
            }
            Request req = new Request("enumeration_element", Request.GET);
            req.addGetParameter("enum_id", enumId);
            Response enumValRes = srv.execute(req);
            if(false == enumValRes.wasSuccessfull())
            {
                log.error("could not read enumeration from server");
                return false;
            }
            for(int i = 0; i < enum_values.size(); i++)
            {
                Element value = enum_values.get(i);
                if(false == checkEnumValues(enumValRes, value, enumId))
                {
                    log.error("Failed to check enumeration element !");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkEnumValues(Response res, Element enumE, int enumId)
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
            int valId = createEnumerationValueOnServer(enumId, svdName, svdDescription, svdValue, svdIsDefault);
            if(0 == valId)
            {
                log.error("could not create a enumeration value !");
                return false;
            }
        }

        return true;
    }

    private int createEnumerationValueOnServer(
            int enumId,
            String name,
            String description,
            String value,
            boolean isDefault)
    {
        Request req = new Request("enumeration_element", Request.POST);
        req.addGetParameter("enum_id", enumId);
        if(null != name)
        {
            req.addGetParameter("name", name);
        }
        if(null != description)
        {
            req.addGetParameter("description", description);
        }
        if(null != value)
        {
            req.addGetParameter("value", value);
        }
        if(true == isDefault)
        {
            req.addGetParameter("isDefault", 1);
        }
        else
        {
            req.addGetParameter("isDefault", 0);
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
        req.addGetParameter("id", valId);
        if(null != name)
        {
            req.addGetParameter("name", name);
        }
        if(null != description)
        {
            req.addGetParameter("description", description);
        }
        if(null != value)
        {
            req.addGetParameter("value", value);
        }
        if(true == isDefault)
        {
            req.addGetParameter("isDefault", 1);
        }
        else
        {
            req.addGetParameter("isDefault", 0);
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

    private int createEnumerationOnServer(int fieldId, String name, String usage)
    {
        Request req = new Request("enumeration", Request.POST);
        req.addGetParameter("field_id", fieldId);
        if(null != name)
        {
            req.addGetParameter("name", name);
        }
        if(null != usage)
        {
            req.addGetParameter("usage_right", usage);
        }
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            return 0;
        }
        else
        {
            return res.getInt("id");
        }
    }

    private boolean updateEnumerationOnServer(int id, String name, String usage)
    {
        Request req = new Request("enumeration", Request.PUT);
        req.addGetParameter("id", id);
        if(null != name)
        {
            req.addGetParameter("name", name);
        }
        if(null != usage)
        {
            req.addGetParameter("usage_right", usage);
        }
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not update the enumeration on the server");
            return false;
        }
        else
        {
            return true;
        }
    }
}
