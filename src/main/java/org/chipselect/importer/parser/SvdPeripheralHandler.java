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
        if(null == svdValue)
        {
            svdValue = allPeripherals.getString(origIdx, serverName);
        }
        if(null == svdValue)
        {
            // value not present
            return null;
        }
        svdValue = svdValue.trim();
        String srvValue = allPeripherals.getString(idx, serverName);
        if(false == svdValue.equals(srvValue))
        {
            log.info("update needed for {} from {} to {}!", svdName, srvValue, svdValue);
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

        if(null !=  peripheral.getChildText("dim"))
        {
            log.error("dim not implemented!");
            return false;
        }
        if(null !=  peripheral.getChildText("dimIncrement"))
        {
            log.error("dimIncrement not implemented!");
            return false;
        }
        if(null !=  peripheral.getChildText("dimIndex"))
        {
            log.error("dimIndex not implemented!");
            return false;
        }
        if(null !=  peripheral.getChildText("dimName"))
        {
            log.error("dimName not implemented!");
            return false;
        }
        if(null !=  peripheral.getChildText("dimArrayIndex"))
        {
            log.error("dimArrayIndex not implemented!");
            return false;
        }
        if(null !=  peripheral.getChildText("alternatePeripheral"))
        {
            log.error("alternatePeripheral not implemented!");
            return false;
        }
        if(null !=  peripheral.getChildText("prependToName"))
        {
            log.error("prependToName not implemented!");
            return false;
        }
        if(null !=  peripheral.getChildText("appendToName"))
        {
            log.error("appendToName not implemented!");
            return false;
        }
        if(null !=  peripheral.getChildText("headerStructName"))
        {
            log.error("headerStructName not implemented!");
            return false;
        }


        // description
        String upd = checkIfUpdateNeeded(idx, origIdx, peripheral, "description", "description");
        if(null != upd)
        {
            log.error("description not implemented!");
            return false;
        }
        // disableCondition
        upd = checkIfUpdateNeeded(idx, origIdx, peripheral, "disableCondition", "disable_Condition");
        if(null != upd)
        {
            log.error("disableCondition not implemented!");
            return false;
        }
        // baseAddress
        upd = checkIfUpdateNeeded(idx, origIdx, peripheral, "baseAddress", "base_address");
        if(null != upd)
        {
            log.error("baseAddress not implemented!");
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
        String svdGroupName = peripheral.getChildText("groupName");
        if(null != svdGroupName)
        {
            if(0 < svdGroupName.length())
            {
                String srvGroupName = srvPeripheral.getString(idx, "group_name");
                if(false == svdGroupName.equals(srvGroupName))
                {
                    log.info("group name changed from {} to {} !", srvGroupName, svdGroupName);
                    log.error("update group name - not implemented!");
                    return false;
                }
            }
        }

        // size
        String svdSize = peripheral.getChildText("size");
        if(null != svdSize)
        {
            default_size = Integer.parseInt(svdSize);
        }
        // access
        String svdAccess = peripheral.getChildText("access");
        if(null != svdAccess)
        {
            default_access = svdAccess;
        }
        // protection
        String svdProtection = peripheral.getChildText("protection");
        if(null != svdProtection)
        {
            default_protection = svdProtection;
        }
        // resetValue
        String svdResetValue = peripheral.getChildText("resetValue");
        if(null != svdResetValue)
        {
            default_resetValue = svdResetValue;
        }
        // resetMask
        String svdResetMask = peripheral.getChildText("resetMask");
        if(null != svdResetMask)
        {
            default_resetMask = svdResetMask;
        }

        // addressBlock
        Element addressBlock = peripheral.getChild("addressBlock");
        if(null !=  addressBlock)
        {
            if(false == updateAddressBlock(peripheralId, addressBlock))
            {
                return false;
            }
        }
        // interrupt
        if(null !=  peripheral.getChildText("interrupt"))
        {
            log.error("interrupt not implemented!");
            return false;
        }
        // registers
        if(null !=  peripheral.getChildText("registers"))
        {
            log.error("registers not implemented!");
            return false;
        }

        return true;
    }

    private boolean updateAddressBlock(int srvPerIdx, Element svdAaddressBlock)
    {
        Response res = srv.get("address_block", "per_id=" + srvPerIdx);
        if(false == res.wasSuccessfull())
        {
            return false;
        }
        // else -> go on

        int offset = -1;
        int size = -1;
        String usage = null;
        String protection = null;


        // check for unknown children
        List<Element> children = svdAaddressBlock.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            // all defined child types from SVD standard
            // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_device.html
            case "offset":
                offset = Integer.decode(child.getText());
                break;
            case "size":
                size = Integer.decode(child.getText());
                break;
            case "usage":
                usage = child.getText();
                break;
            case "protection":
                protection = child.getText();
                break;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown addressblock child tag: {}", name);
                return false;
            }
        }

        int srvOffset = res.getInt("address_offset");
        int srvSize = res.getInt("size");
        String srvUsage = res.getString("mem_usage");
        String srvProtection = res.getString("protection");
        // check for changes
        if(    (offset != srvOffset)
            || (size != srvSize)
            || ((null != usage) && (false == usage.equals(srvUsage)))
            || ((null != protection) && (false == protection.equals(srvProtection)))
            )
        {
            log.trace("offset : new: {} old : {}", offset, srvOffset);
            log.trace("size : new: {} old : {}", size, srvSize);
            log.trace("usage : new: {} old : {}", usage, srvUsage);
            log.trace("protection : new: {} old : {}", protection, srvProtection);
            log.error("update addressblock not implemented!");
            return false;
        }
        // else  no changes

        return true;
    }

    private boolean createPeripheralInstanceFrom(Element peripheral)
    {
        log.error("createPeripheralInstanceFrom not implemented!");
        return false;
    }

}
