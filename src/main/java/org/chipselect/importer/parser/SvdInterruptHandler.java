package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.Tool;
import org.chipselect.importer.server.Request;
import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdInterruptHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;

    public SvdInterruptHandler(Server srv)
    {
        this.srv = srv;
    }

    public boolean updateInterrupt(Element svdPeripheral, int srvPeripheralInstanceId)
    {
        if(0 == srvPeripheralInstanceId)
        {
            log.error("Peripheral Instance ID invalid !");
            return false;
        }
        Request req = new Request("interrupt", Request.GET);
        req.addGetParameter("per_in_id", srvPeripheralInstanceId);
        Response interruptRes = srv.execute(req);
        if(false == interruptRes.wasSuccessfull())
        {
            return false;
        }
        // else -> go on
        List<Element>  interruptChildren = svdPeripheral.getChildren("interrupt");
        for(Element interrupt : interruptChildren)
        {
            if(false == checkInterrupt(interruptRes, interrupt, srvPeripheralInstanceId))
            {
                return false;
            }
        }
        return true;
    }

    public boolean updateDerivedInterrupt(Element svdDerivedPeripheral, Element svdOriginalPeripheral,
            int srvPeripheralInstanceId)
    {
        log.trace("Derived peripheral");
        if(0 == srvPeripheralInstanceId)
        {
            log.error("Peripheral Instance ID invalid !");
            return false;
        }
        Request req = new Request("interrupt", Request.GET);
        req.addGetParameter("per_in_id", srvPeripheralInstanceId);
        Response interruptRes = srv.execute(req);
        if(false == interruptRes.wasSuccessfull())
        {
            return false;
        }
        // else -> go on
        List<Element>  interruptChildren = svdDerivedPeripheral.getChildren("interrupt");
        if(true == interruptChildren.isEmpty())
        {
            log.trace("derived peripheral does not have an interrupt child eleemnt.");
            interruptChildren = svdOriginalPeripheral.getChildren("interrupt");
        }
        // else the derive interrupt block overwrites the original block!
        for(Element interrupt : interruptChildren)
        {
            if(false == checkInterrupt(interruptRes, interrupt, srvPeripheralInstanceId))
            {
                return false;
            }
        }
        return true;
    }

    private boolean checkInterrupt(Response res, Element svdInterrupt, int peripheralInstanceId)
    {
        String irqName = null;
        String description = null;
        long number = -1;

        // check for unknown children
        List<Element> children = svdInterrupt.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            // all defined child types from SVD standard
            // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_device.html
            case "name":
                irqName = Tool.cleanupString(child.getText());
                break;

            case "description":
                description = Tool.cleanupString(child.getText());
                break;

            case "value":
                number = Tool.decode(child.getText());
                break;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown interrupt child tag: {}", name);
                return false;
            }
        }

        log.trace("checking Interrupt {}", irqName);

        boolean found = false;
        int numInterruptServer = res.numResults();
        for(int i = 0; i < numInterruptServer; i++)
        {
            String srvName = res.getString(i, "name");
            String srvDescription = res.getString(i, "description");
            long srvNumber = res.getLong(i, "number");

            if((null != irqName) && (true == irqName.equals(srvName)))
            {
                found = true;
                // check for Change
                boolean changed = false;

                if((null != description) && (false == description.equals(srvDescription)))
                {
                    log.trace("description changed from :{}: to :{}:", srvDescription, description);
                    changed = true;
                }
                // else no change
                if((number != -1) && (srvNumber != number))
                {
                    log.trace("number changed from :{}: to :{}:", srvNumber, number);
                    changed = true;
                }
                // else no change
                if(true == changed)
                {
                    if(false == updateSrvInterrupt(res.getInt(i, "id"), irqName, description, number))
                    {
                        return false;
                    }
                }
                // else no change -> no update needed
                break;
            }
        }
        if(false == found)
        {
            log.trace("created new interrupt on server: name = {}, description = {}", irqName, description);
            Request req = new Request("interrupt", Request.POST);
            req.addGetParameter("per_in_id", peripheralInstanceId);
            if(null != irqName)
            {
                req.addGetParameter("name", irqName);
            }
            if(null != description)
            {
                req.addGetParameter("description", description);
            }
            req.addGetParameter("number", number);
            Response postRes = srv.execute(req);
            if(false == postRes.wasSuccessfull())
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        else
        {
            return true;
        }
    }

    private boolean updateSrvInterrupt(int id, String irqName, String description, long number)
    {
        Request req = new Request("interrupt", Request.PUT);
        req.addGetParameter("id", id);
        if(null != irqName)
        {
            req.addGetParameter("name", irqName);
        }
        if(null != description)
        {
            req.addGetParameter("description", description);
        }
        req.addGetParameter("number", number);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

}
