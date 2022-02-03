package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdPeripheralHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private int deviceId = 0;
    private int default_size = 0;
    private String default_access = null;
    private String default_resetValue = null;
    private String default_resetMask = null;
    private String default_protection = null;
    private Response allPeripherals = null;

    public SvdPeripheralHandler(Server srv)
    {
        this.srv = srv;
    }

    public void setId(int dev_id)
    {
        deviceId = dev_id;
    }

    public void setDefaultSize(int default_size)
    {
        this.default_size = default_size;
    }

    public void setDefaultAccess(String default_access)
    {
        this.default_access = default_access;
    }

    public void setDefaultResetValue(String default_resetValue)
    {
        this.default_resetValue = default_resetValue;
    }

    public void setDefaultResetMask(String default_resetMask)
    {
        this.default_resetMask = default_resetMask;
    }

    public void setDefaultProtection(String default_protection)
    {
        this.default_protection = default_protection;
    }

    private void getPeripheralInstancesFromServer()
    {
        if(0 == deviceId)
        {
            allPeripherals = null;
            return;
        }
        Response res = srv.get("peripheral_instance", "dev_id=" + deviceId);
        if(false == res.wasSuccessfull())
        {
            allPeripherals = null;
            return;
        }
        allPeripherals = res;
    }

    private Response getPeripheralFromServer(int peripheralId)
    {
        Response res = srv.get("peripheral", "id=" + peripheralId);
        if(false == res.wasSuccessfull())
        {
            return null;
        }
        else
        {
            return res;
        }
    }

    private int getPeripheralIndexFor(String name)
    {
        for(int i = 0; i < allPeripherals.numResults(); i++)
        {
            String svdName = allPeripherals.getString(i, "name");
            if(false == svdName.equals(name))
            {
                // log.trace("Name mismatch: {} - {}", svdName, name);
                continue;
            }
            else
            {
                return i;
            }
        }
        // not found
        return -1;
    }


    public boolean handle(Element peripheral)
    {
        if(null == allPeripherals)
        {
            getPeripheralInstancesFromServer();
        }
        if(null == allPeripherals)
        {
            log.error("Could not read device peripherals from sever");
            return false;
        }
        String name = peripheral.getChildText("name");
        log.trace("Peripheral: {}", name);
        int idx = getPeripheralIndexFor(name);
        if(0 > idx)
        {
            // new peripheral
            log.trace("creating new peripheral {}", name);
            return createPeripheralInstanceFrom(peripheral);
        }
        else
        {
            return updatePeripheral(idx, peripheral);
        }
    }

    private String checkIfUpdateNeeded(int idx, int origIdx, Element peripheral, String svdName, String serverName)
    {
        String svdValue = peripheral.getChildText(svdName);
        if( null == svdValue)
        {
            svdValue = allPeripherals.getString(origIdx, serverName);
        }
        if( null == svdValue)
        {
            // value not present
            return null;
        }
        svdValue = svdValue.trim();
        String srvValue = allPeripherals.getString(idx, serverName);
        if(false == svdValue.equals(srvValue))
        {
            log.info("updating {} from {} to {}!", svdName, srvValue, svdValue);
            return svdValue;
        }
        // else value is the same so no update needed
        return null;
    }

    private boolean checkIfValidPeripheral(Element peripheral)
    {
        // check for unknown children
        List<Element> children = peripheral.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            // all defined child types from SVD standard
            // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_device.html
            case "access":
            case "addressBlock":
            case "alternatePeripheral":
            case "appendToName":
            case "baseAddress":
            case "description":
            case "dim":
            case "dimIncrement":
            case "dimIndex":
            case "dimName":
            case "dimArrayIndex":
            case "disableCondition":
            case "groupName":
            case "headerStructName":
            case "interrupt":
            case "name":
            case "prependToName":
            case "protection":
            case "registers":
            case "resetValue":
            case "resetMask":
            case "size":
            case "version":
                continue;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown child tag: {}", name);
                return false;
            }
        }
        return true;
    }


    private boolean updatePeripheral(int idx, Element peripheral)
    {
        // check if format is valied
        if(false == checkIfValidPeripheral(peripheral))
        {
            return false;
        }
        // check if this is a derived peripheral
        String original = peripheral.getAttributeValue("derivedFrom");
        int origIdx = getPeripheralIndexFor(original);

        // name - already handled
        // ignoring  <version></version>

        // TODO:
        // dim
        // dimIncrement
        // dimIndex
        // dimName
        // dimArrayIndex
        // alternatePeripheral
        // prependToName
        // appendToName
        // headerStructName


        // description
        String upd = checkIfUpdateNeeded(idx, origIdx, peripheral, "description", "description");
        if(null != upd)
        {
            log.error("not implemented!");
            return false;
        }
        // disableCondition
        upd = checkIfUpdateNeeded(idx, origIdx, peripheral, "disableCondition", "disable_Condition");
        if(null != upd)
        {
            log.error("not implemented!");
            return false;
        }
        // baseAddress
        upd = checkIfUpdateNeeded(idx, origIdx, peripheral, "baseAddress", "base_address");
        if(null != upd)
        {
            log.error("not implemented!");
            return false;
        }

        // peripheral
        int peripheralId = allPeripherals.getInt(idx, "peripheral_id");
        if(0 == peripheralId)
        {
            // peripheral not on server -> create new peripheral
            log.error("no peripheral - not implemented!");
            return false;
        }
        Response srvPeripheral = getPeripheralFromServer(peripheralId);
        if(null == srvPeripheral)
        {
            log.error("could not read peripheral information from server!");
            return false;
        }
        // groupName
        // TODO

        // size
        // access
        // protection
        // resetValue
        // resetMask

        // addressBlock
        // interrupt
        // registers



        log.error("missing - not implemented!");
        return false;
    }

    private boolean createPeripheralInstanceFrom(Element peripheral)
    {
        log.error("not implemented!");
        return false;
    }

}
