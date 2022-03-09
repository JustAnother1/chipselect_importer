package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.Tool;
import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdPeripheralHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private final SvdAddressBlockHandler addressBlockHandler;
    private final SvdInterruptHandler interruptHandler;
    private final SvdRegisterHandler registerHandler;
    private int default_size = 0;
    private String default_access = null;
    private String default_resetValue = null;
    private String default_resetMask = null;
    private String default_protection = null;
    private Response srvAllPeripherals = null;

    public SvdPeripheralHandler(Server srv)
    {
        this.srv = srv;
        addressBlockHandler = new SvdAddressBlockHandler(srv);
        interruptHandler = new SvdInterruptHandler(srv);
        registerHandler = new SvdRegisterHandler(srv);
    }

    public void setDefaultSize(int default_size)
    {
        this.default_size = default_size;
        addressBlockHandler.setDefaultSize(this.default_size);
    }

    public void setDefaultAccess(String default_access)
    {
        this.default_access = default_access;
        registerHandler.setDefaultAccess(this.default_access);
    }

    public void setDefaultResetValue(String default_resetValue)
    {
        this.default_resetValue = default_resetValue;
        registerHandler.setDefaultResetValue(this.default_resetValue);
    }

    public void setDefaultResetMask(String default_resetMask)
    {
        this.default_resetMask = default_resetMask;
        registerHandler.setDefaultResetMask(this.default_resetMask);
    }

    public void setDefaultProtection(String default_protection)
    {
        this.default_protection = default_protection;
        addressBlockHandler.setDefaultProtection(this.default_protection);
    }

    /**
     *
     * @param peripheral may not be a derived peripheral
     * @return true = success, false = error
     */
    public boolean handle(Element peripheral)
    {
        String name = peripheral.getChildText("name");
        log.trace("Peripheral: {}", name);
        int srvIdx = getPeripheralSrvIndexFor(name);
        if(0 > srvIdx)
        {
            // new peripheral
            log.trace("creating new peripheral {}", name);
            return createPeripheralInstanceFrom(peripheral);
        }
        else
        {
            // check if format is valid
            if(false == checkIfValidPeripheral(peripheral))
            {
                return false;
            }
            // this peripheral is by definition not derived !
            return updateIndependentPeripheral(srvIdx, peripheral);
        }
    }

    /**
     *
     * @param svdDerivedPeripheral the derived peripheral
     * @param svdOriginalPeripheral the peripheral that it was derived from
     * @return true = success, false = error
     */
    public boolean handleDerived(Element svdDerivedPeripheral, Element svdOriginalPeripheral)
    {
        String name = svdDerivedPeripheral.getChildText("name");
        log.trace("Peripheral: {}", name);
        int srvIdx = getPeripheralSrvIndexFor(name);
        if(0 > srvIdx)
        {
            // new peripheral
            log.trace("creating new peripheral {}", name);
            return createPeripheralInstanceFromDerived(svdDerivedPeripheral, svdOriginalPeripheral);
        }
        else
        {
            // check if format is valid
            if(false == checkIfValidPeripheral(svdDerivedPeripheral))
            {
                return false;
            }
            // this peripheral is derived by definition !
            return updateDerivedPeripheral(srvIdx, svdDerivedPeripheral, svdOriginalPeripheral);
        }
    }

    public boolean getAllPeripheralInstancesFromServer(int srvDeviceId)
    {
        if(0 == srvDeviceId)
        {
            srvAllPeripherals = null;
            return false;
        }
        Response res = srv.get("peripheral_instance", "dev_id=" + srvDeviceId);
        if(false == res.wasSuccessfull())
        {
            srvAllPeripherals = null;
            return false;
        }
        srvAllPeripherals = res;
        return true;
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

    private int getPeripheralSrvIndexFor(String name)
    {
        for(int i = 0; i < srvAllPeripherals.numResults(); i++)
        {
            String svdName = srvAllPeripherals.getString(i, "name");
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

    private boolean updateIndependentPeripheral(int idx, Element peripheral)
    {
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
        String svdDescriptionValue = peripheral.getChildText("description");
        if(null == svdDescriptionValue)
        {
            // value not present -> OK
        }
        else
        {
            svdDescriptionValue = Tool.cleanupString(svdDescriptionValue);
            String srvValue = srvAllPeripherals.getString(idx, "description");
            if(false == svdDescriptionValue.equals(srvValue))
            {
                log.info("update needed for {} from :{}: to :{}:!", "description", srvValue, svdDescriptionValue);
                log.error("description not implemented!");
                return false;
            }
            // else value is the same so no update needed -> OK
        }

        // disableCondition
        // String checkIfUpdateNeeded(int idx, int origIdx, Element peripheral, String svdName, String serverName)
        String svdValue = peripheral.getChildText("disableCondition");
        if(null == svdValue)
        {
            // value not present -> OK
        }
        else
        {
            svdValue = Tool.cleanupString(svdValue);
            String srvValue = srvAllPeripherals.getString(idx, "disable_Condition");
            if(false == svdValue.equals(srvValue))
            {
                log.info("update needed for {} from :{}: to :{}:!", "disableCondition", srvValue, svdValue);
                log.error("disableCondition not implemented!");
                return false;
            }
            // else value is the same so no update needed -> OK
        }

        // baseAddress
        String strBaseAddress = peripheral.getChildText("baseAddress");
        long baseAddress = Long.decode(strBaseAddress);
        String strSrvBaseAddress = srvAllPeripherals.getString(idx, "base_address");
        long srvBaseAddress = Long.decode(strSrvBaseAddress);
        if(baseAddress != srvBaseAddress)
        {
            log.trace("baseAddress = {}", strBaseAddress);
            log.trace("baseAddress = {}", baseAddress);
            log.trace("srvBaseAddress = {}", strSrvBaseAddress);
            log.trace("srvBaseAddress = {}", srvBaseAddress);
            log.error("update baseAddress not implemented!");
            return false;
        }

        // peripheral
        int peripheralId = srvAllPeripherals.getInt(idx, "peripheral_id");
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
                String srvGroupName = srvPeripheral.getString("group_name");
                if(false == svdGroupName.equals(srvGroupName))
                {
                    log.info("group name changed from :{}: to :{}: !", srvGroupName, svdGroupName);
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
        if(false == addressBlockHandler.updateAddressBlock(peripheral, peripheralId))
        {
            return false;
        }

        // interrupt
        if(false == interruptHandler.updateInterrupt(peripheral, peripheralId))
        {
            return false;
        }

        // registers
        if(false == registerHandler.updateRegister(peripheral, peripheralId))
        {
            return false;
        }

        // all done
        return true;
    }

    private boolean updateDerivedPeripheral(int srvIdx, Element svdDerivedPeripheral, Element svdOriginalPeripheral)
    {
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

        if(null !=  svdDerivedPeripheral.getChildText("dim"))
        {
            log.error("dim not implemented!");
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimIncrement"))
        {
            log.error("dimIncrement not implemented!");
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimIndex"))
        {
            log.error("dimIndex not implemented!");
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimName"))
        {
            log.error("dimName not implemented!");
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimArrayIndex"))
        {
            log.error("dimArrayIndex not implemented!");
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("alternatePeripheral"))
        {
            log.error("alternatePeripheral not implemented!");
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("prependToName"))
        {
            log.error("prependToName not implemented!");
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("appendToName"))
        {
            log.error("appendToName not implemented!");
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("headerStructName"))
        {
            log.error("headerStructName not implemented!");
            return false;
        }

        // description
        String svdDescriptionValue = svdDerivedPeripheral.getChildText("description");
        if(null == svdDescriptionValue)
        {
            svdDescriptionValue = svdOriginalPeripheral.getChildText("description");
        }
        if(null == svdDescriptionValue)
        {
            // value not present -> OK
        }
        else
        {
            svdDescriptionValue = Tool.cleanupString(svdDescriptionValue);
            String srvValue = srvAllPeripherals.getString(srvIdx, "description");
            if(false == svdDescriptionValue.equals(srvValue))
            {
                log.info("update needed for {} from :{}: to :{}:!", "description", srvValue, svdDescriptionValue);
                log.error("update description not implemented!");
                return false;
            }
            // else value is the same so no update needed -> OK
        }

        // disableCondition
        // private String checkIfUpdateNeeded(int idx, int origIdx, Element peripheral, String svdName, String serverName)
        String svdValue = svdDerivedPeripheral.getChildText("disableCondition");
        if(null == svdValue)
        {
            svdValue = svdOriginalPeripheral.getChildText("disableCondition");
        }
        if(null == svdValue)
        {
            // value not present -> OK
        }
        else
        {
            svdValue = Tool.cleanupString(svdValue);
            String srvValue = srvAllPeripherals.getString(srvIdx, "disable_Condition");
            if(false == svdValue.equals(srvValue))
            {
                log.info("update needed for {} from :{}: to :{}:!", "disableCondition", srvValue, svdValue);
                log.error("update disableCondition not implemented!");
                return false;
            }
            // else value is the same so no update needed -> OK
        }

        // baseAddress
        String strBaseAddress = svdDerivedPeripheral.getChildText("baseAddress");
        long baseAddress = Long.decode(strBaseAddress);
        String strSrvBaseAddress = srvAllPeripherals.getString(srvIdx, "base_address");
        long srvBaseAddress = Long.decode(strSrvBaseAddress);
        if(baseAddress != srvBaseAddress)
        {
            log.trace("baseAddress = {}", strBaseAddress);
            log.trace("baseAddress = {}", baseAddress);
            log.trace("srvBaseAddress = {}", strSrvBaseAddress);
            log.trace("srvBaseAddress = {}", srvBaseAddress);
            log.error("update baseAddress not implemented!");
            return false;
        }

        // peripheral
        int peripheralId = srvAllPeripherals.getInt(srvIdx, "peripheral_id");
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
        String svdGroupName = svdDerivedPeripheral.getChildText("groupName");
        if(null == svdGroupName)
        {
            svdGroupName = svdOriginalPeripheral.getChildText("groupName");
        }
        if(null != svdGroupName)
        {
            if(1 > svdGroupName.length())
            {
                svdGroupName = svdOriginalPeripheral.getChildText("groupName");
            }
            if(0 < svdGroupName.length())
            {
                String srvGroupName = srvPeripheral.getString("group_name");
                if(false == svdGroupName.equals(srvGroupName))
                {
                    log.info("group name changed from :{}: to :{}: !", srvGroupName, svdGroupName);
                    log.error("update group name - not implemented!");
                    return false;
                }
                // else matches -> no change necessary
            }
            // else no group name given -> OK
        }
        // else no group name given -> OK

        // size
        String svdSize = svdDerivedPeripheral.getChildText("size");
        if(null != svdSize)
        {
            default_size = Integer.parseInt(svdSize);
        }
        // access
        String svdAccess = svdDerivedPeripheral.getChildText("access");
        if(null != svdAccess)
        {
            default_access = svdAccess;
        }
        // protection
        String svdProtection = svdDerivedPeripheral.getChildText("protection");
        if(null != svdProtection)
        {
            default_protection = svdProtection;
        }
        // resetValue
        String svdResetValue = svdDerivedPeripheral.getChildText("resetValue");
        if(null != svdResetValue)
        {
            default_resetValue = svdResetValue;
        }
        // resetMask
        String svdResetMask = svdDerivedPeripheral.getChildText("resetMask");
        if(null != svdResetMask)
        {
            default_resetMask = svdResetMask;
        }

        // "per_in_id" == srvIdx
        // peripheralId = peripheralId
        // addressBlock
        if(false == addressBlockHandler.updateDerivedAddressBlock(svdDerivedPeripheral, svdOriginalPeripheral, peripheralId)) // peripheral
        {
            return false;
        }

        // interrupt
        if(false == interruptHandler.updateDerivedInterrupt(svdDerivedPeripheral, svdOriginalPeripheral, srvIdx))
        {
            return false;
        }

        // registers
        if(false == registerHandler.updateDerivedRegister(svdDerivedPeripheral, svdOriginalPeripheral, peripheralId))
        {
            return false;
        }

        // all done
        return true;
    }

    private boolean createPeripheralInstanceFrom(Element peripheral)
    {
        log.error("createPeripheralInstanceFrom not implemented!");
        return false;
    }

    private boolean createPeripheralInstanceFromDerived(Element svdDerivedPeripheral, Element svdOriginalPeripheral)
    {
        log.error("createPeripheralInstanceFromDerived not implemented!");
        return false;
    }

}
