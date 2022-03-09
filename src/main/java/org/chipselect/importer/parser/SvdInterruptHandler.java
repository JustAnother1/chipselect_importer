package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.Tool;
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
        Response interruptRes = srv.get("interrupt", "per_in_id=" + srvPeripheralInstanceId);
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
        Response interruptRes = srv.get("interrupt", "per_in_id=" + srvPeripheralInstanceId);
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
        int number = -1;

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
                number = Integer.decode(child.getText());
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
            int srvNumber = res.getInt(i, "number");

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
            String param = "per_in_id=" + peripheralInstanceId + "&name=" + irqName + "&description=" + description + "&number=" + number;
            Response postRes = srv.post("interrupt", param);
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

    private boolean updateSrvInterrupt(int id, String irqName, String description, int number)
    {
        String param = "id=" + id + "&name=" + irqName + "&description=" + description + "&number=" + number;
        Response res = srv.put("interrupt", param);
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
