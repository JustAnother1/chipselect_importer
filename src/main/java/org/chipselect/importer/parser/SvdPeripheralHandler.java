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
        svdValue = Tool.cleanupString(svdValue);
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
        // check if format is valid
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
        String strBaseAddress = peripheral.getChildText("baseAddress");
        long baseAddress = Long.decode(strBaseAddress);
        String strSrvBaseAddress = allPeripherals.getString(idx, "base_address");
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
                String srvGroupName = srvPeripheral.getString("group_name");
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
        Response AddrBlockRes = srv.get("address_block", "per_id=" + peripheralId);
        if(false == AddrBlockRes.wasSuccessfull())
        {
            return false;
        }
        // else -> go on
        List<Element> AddrBlockchildren = peripheral.getChildren("addressBlock");
        for(Element addressBlock : AddrBlockchildren)
        {
            if(false == checkAddressBlock(AddrBlockRes, addressBlock))
            {
                return false;
            }
        }

        // interrupt
        Response interruptRes = srv.get("interrupt", "per_in_id=" + peripheralId);
        if(false == interruptRes.wasSuccessfull())
        {
            return false;
        }
        // else -> go on
        List<Element>  interruptChildren = peripheral.getChildren("interrupt");
        for(Element interrupt : interruptChildren)
        {
            if(false == checkInterrupt(interruptRes, interrupt, peripheralId))
            {
                return false;
            }
        }

        // registers
        Element registers = peripheral.getChild("registers");
        if(null !=  registers)
        {
            Response res = srv.get("register", "per_id=" + peripheralId);
            if(false == res.wasSuccessfull())
            {
                return false;
            }
            // else -> go on
            List<Element> children = registers.getChildren();
            for(Element child : children)
            {
                String name = child.getName();
                switch(name)
                {
                // all defined child types from SVD standard
                // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_device.html
                case "cluster":
                    log.error("cluster not implemented!");
                    break;

                case "register":
                    if(false == checkRegister(res, child))
                    {
                        return false;
                    }
                    break;

                default:
                    // undefined child found. This is not a valid SVD file !
                    log.error("Unknown registers child tag: {}", name);
                    return false;
                }
            }
        }

        // all done
        return true;
    }

    private boolean checkRegister(Response res, Element svdRegisters)
    {
        String name = null;
        String displayName = null;
        String description = null;
        String addressOffset = null;
        int size = -1;
        String access = null;
        String reset_value = null;
        String alternate_register = null;
        String reset_Mask = null;
        String read_action = null;
        String modified_write_values = null;
        String data_type = null;
        Element fields = null;

        // check for unknown children
        List<Element> children = svdRegisters.getChildren();
        for(Element child : children)
        {
            String tagName = child.getName();
            switch(tagName)
            {
            // all defined child types from SVD standard
            // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_device.html

            case "name" :
                name = child.getText();
                break;

            case "displayName" :
                displayName = Tool.cleanupString(child.getText());
                break;

            case "description" :
                description = Tool.cleanupString(child.getText());
                break;

            case "alternateRegister" :
                alternate_register = child.getText();
                break;

            case "addressOffset" :
                addressOffset = child.getText();
                break;

            case "size" :
                size = Integer.decode(child.getText());
                break;

            case "access" :
                access = child.getText();
                break;

            case "resetValue" :
                reset_value = child.getText();
                break;

            case "resetMask" :
                reset_Mask = child.getText();
                break;

            case "dataType" :
                data_type = child.getText();
                break;

            case "modifiedWriteValues" :
                modified_write_values = child.getText();
                break;

            case "readAction" :
                read_action = child.getText();
                break;

            case "fields" :
                fields = child;
                break;

            case "dim" :
            case "dimIncrement":
            case "dimIndex" :
            case "dimName" :
            case "dimArrayIndex" :
            case "alternateGroup" :
            case "protection" :
            case "writeConstraint" :
                log.error("Register child {} not implemented!", name);
                return false;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown register child tag: {}", tagName);
                return false;
            }
        }

        int srvId = -1;
        log.trace("checking register {}", name);

        boolean found = false;
        int numRegisterServer = res.numResults();
        for(int i = 0; i < numRegisterServer; i++)
        {
            String srvName = res.getString(i, "name");

            if((null != name) && (true == name.equals(srvName)))
            {
                found = true;
                srvId = res.getInt(i,  "id");
                log.trace("found register {} ({})", name, srvId);
                String srvDisplayName = res.getString(i, "display_name");
                String srvDescription = res.getString(i, "description");
                String srvAddressOffset = res.getString(i, "address_offset");
                int    srvSize = res.getInt(i,  "size");
                String srvAccess = res.getString(i, "access");
                String srvReset_value = res.getString(i, "reset_value");
                String srvAlternate_register = res.getString(i, "alternate_register");
                String srvReset_Mask = res.getString(i, "reset_Mask");
                String srvRead_action = res.getString(i, "read_action");
                String srvModified_write_values = res.getString(i, "modified_write_values");
                String srvData_type = res.getString(i, "data_type");

                // check for Change
                boolean changed = false;
                if((null != displayName) && (false == "".equals(displayName)) && (false == displayName.equals(srvDisplayName)))
                {
                    log.trace("display name changed from {} to {}", srvDisplayName, displayName);
                    changed = true;
                }
                // else no change
                if((null != description) && (false == "".equals(description)) && (false == description.equals(srvDescription)))
                {
                    log.trace("description changed from {} to {}", srvDescription, description);
                    changed = true;
                }
                // else no change
                if((null != addressOffset) && (false == "".equals(addressOffset)) && (false == addressOffset.equals(srvAddressOffset)))
                {
                    long val = Long.decode(addressOffset);
                    long srvVal = Long.decode(srvAddressOffset);
                    if(val != srvVal)
                    {
                        log.trace("address offset changed from {} to {}", srvAddressOffset, addressOffset);
                        changed = true;
                    }
                    // else "0" is not different to "0x00"
                }
                // else no change
                if((size != -1) && (size != srvSize))
                {
                    log.trace("size changed from {} to {}", srvSize, size);
                    changed = true;
                }
                // else no change
                if((null != access) && (false == "".equals(access)) && (false == access.equals(srvAccess)))
                {
                    log.trace("access changed from {} to {}", srvAccess, access);
                    changed = true;
                }
                // else no change
                if((null != reset_value) && (false == "".equals(reset_value)) && (false == reset_value.equals(srvReset_value)))
                {
                    long val = Long.decode(reset_value);
                    long srvVal = Long.decode(srvReset_value);
                    if(val != srvVal)
                    {
                        log.trace("reset value changed from {} to {}", srvReset_value, reset_value);
                        log.trace("reset value changed from {} to {}", srvVal, val);
                        changed = true;
                    }
                    // else "0" is not different to "0x00"
                }
                // else no change
                if((null != alternate_register) && (false == "".equals(alternate_register)) && (false == alternate_register.equals(srvAlternate_register)))
                {
                    log.trace("alternate register changed from {} to {}", srvAlternate_register, alternate_register);
                    changed = true;
                }
                // else no change
                if((null != reset_Mask) && (false == "".equals(reset_Mask)) && (false == reset_Mask.equals(srvReset_Mask)))
                {
                    long val = Long.decode(reset_Mask);
                    long srvVal = Long.decode(srvReset_Mask);
                    if(val != srvVal)
                    {
                        log.trace("reset mask changed from {} to {}", srvReset_Mask, reset_Mask);
                        changed = true;
                    }
                    // else "0" is not different to "0x00"
                }
                // else no change
                if((null != read_action) && (false == "".equals(read_action)) && (false == read_action.equals(srvRead_action)))
                {
                    log.trace("read action changed from {} to {}", srvRead_action, read_action);
                    changed = true;
                }
                // else no change
                if((null != modified_write_values) && (false == "".equals(modified_write_values)) && (false == modified_write_values.equals(srvModified_write_values)))
                {
                    log.trace("modified write values changed from {} to {}", srvModified_write_values, modified_write_values);
                    changed = true;
                }
                // else no change
                if((null != data_type) && (false == "".equals(data_type)) && (false == data_type.equals(srvData_type)))
                {
                    log.trace("data type changed from {} to {}", srvData_type, data_type);
                    changed = true;
                }
                // else no change

                if(true == changed)
                {
                    log.error("update register not implemented!");
                    return false;
                }
                // else no change -> no update needed
                break;
            }
        }

        if(false == found)
        {
            log.error("update register not implemented!");
            return false;
        }
        else
        {
            if(null != fields)
            {
                Response fieldstRes = srv.get("field", "reg_id=" + srvId);
                if(false == fieldstRes.wasSuccessfull())
                {
                    return false;
                }
                // else -> go on
                List<Element> fieldList = fields.getChildren();
                for(Element field : fieldList)
                {
                    if(false == checkField(fieldstRes, field))
                    {
                        return false;
                    }
                }
            }
            // else no fields in this register :-(
            return true;
        }
    }

    private boolean checkField(Response res, Element field)
    {
        String svdName = null;
        String description = null;
        int bitOffset = -1;
        int sizeBit = -1;
        String access = null;
        String modifiedWriteValues = null;
        String readAction = null;
        String resetValue = null;
        Element enumeration = null;

        // check for unknown children
        List<Element> children = field.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            // all defined child types from SVD standard
            // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_device.html

            case "name":
                svdName = child.getText();
                break;

            case "description":
                description = Tool.cleanupString(child.getText());
                break;

            case "bitOffset":
                bitOffset = Integer.decode(child.getText());
                break;

            case "bitWidth":
                sizeBit= Integer.decode(child.getText());
                break;

            case "lsb":
                bitOffset = Integer.decode(child.getText());
                if(-1 != sizeBit)
                {
                    // we already know msb, so fix the assumption of lsb = 0
                    sizeBit = sizeBit - bitOffset;
                }
                break;

            case "msb":
                if(-1 == bitOffset)
                {
                    // we do not know lsb yet -> assume lsb = 0
                    sizeBit = Integer.decode(child.getText());
                }
                else
                {
                    sizeBit = Integer.decode(child.getText()) - bitOffset;
                }
                break;

            case "bitRange":
                // bitRange value is [17:8] or [1:1]
                String range = child.getText();
                range = range.trim();
                range = range.substring(1, range.length() -2); // remove // []
                String[] parts = range.split(":");
                bitOffset = Integer.decode(parts[1]);
                sizeBit = Integer.decode(parts[0]) + 1;
                break;

            case "access":
                access = child.getText();
                break;

            case "modifiedWriteValues":
                modifiedWriteValues = child.getText();
                break;

            case "readAction":
                readAction = child.getText();
                break;

            case "writeConstraint":
                // limits allowable write values, what if I write something else? does the chip explode?
                // -> ignore for now.
                break;

            case "enumeratedValues":
                enumeration = child;
                break;


            case "dim":
            case "dimIncrement":
            case "dimIndex":
            case "dimName":
            case "dimArrayIndex":
                log.error("field child {} not implemented!", name);
                return false;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown interrupt child tag: {}", name);
                return false;
            }
        }
        log.trace("checking field {}", svdName);

        int srvId = -1;
        boolean found = false;
        int numFieldsServer = res.numResults();
        for(int i = 0; i < numFieldsServer; i++)
        {
            String srvName = res.getString(i, "name");

            if((null != svdName) && (true == svdName.equals(srvName)))
            {
                found = true;
                srvId = res.getInt(i,  "id");
                log.trace("found field {} ({})", svdName, srvId);
                String srvDescription = res.getString(i, "description");
                int    srvBitOffset = res.getInt(i,  "bit_offset");
                int    srvSizeBit = res.getInt(i,  "size_bit");
                String srvAccess = res.getString(i, "access");
                String srvModifiedWriteValues = res.getString(i, "modified_write_values");
                String srvReadAction = res.getString(i, "read_action");
                String srvResetValue = res.getString(i, "reset_value");
                // check for Change
                boolean changed = false;
                if((null != description) && (false == "".equals(description)) && (false == description.equals(srvDescription)))
                {
                    log.trace("description changed from {} to {}", srvDescription, description);
                    changed = true;
                }
                // else no change
                if((bitOffset != -1) && (bitOffset != srvBitOffset))
                {
                    log.trace("bit Offset changed from {} to {}", srvBitOffset, bitOffset);
                    changed = true;
                }
                // else no change
                if((sizeBit != -1) && (sizeBit != srvSizeBit))
                {
                    log.trace("size_bit changed from {} to {}", srvSizeBit, sizeBit);
                    changed = true;
                }
                // else no change
                if((null != access) && (false == "".equals(access)) && (false == access.equals(srvAccess)))
                {
                    log.trace("access changed from {} to {}", srvAccess, access);
                    changed = true;
                }
                // else no change
                if((null != modifiedWriteValues) && (false == "".equals(modifiedWriteValues)) && (false == modifiedWriteValues.equals(srvModifiedWriteValues)))
                {
                    log.trace("modified write values changed from {} to {}", srvModifiedWriteValues, modifiedWriteValues);
                    changed = true;
                }
                // else no change
                if((null != readAction) && (false == "".equals(readAction)) && (false == readAction.equals(srvReadAction)))
                {
                    log.trace("read action changed from {} to {}", srvReadAction, readAction);
                    changed = true;
                }
                // else no change
                if((null != resetValue) && (false == "".equals(resetValue)) && (false == resetValue.equals(srvResetValue)))
                {
                    log.trace("reset value changed from {} to {}", srvResetValue, resetValue);
                    changed = true;
                }
                // else no change

                if(true == changed)
                {
                    log.error("update field not implemented!");
                    return false;
                }
                // else no change -> no update needed
                break;
            }
        }
        if(false == found)
        {
            // this field is missing on the server -> add it
            log.error("update field not implemented!");
            return false;
        }
        else
        {
            // field handeled, -> enums?
            if(null != enumeration)
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
            }
            return true;
        }
    }

    private boolean checkEnumeration(Response enumRes, Element enumE)
    {
        log.error("check enumeration not implemented!");
        return false;
    }

    private boolean checkInterrupt(Response res, Element svdInterrupt, int peripheralId)
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
                irqName = child.getText();
                break;

            case "description":
                description = child.getText();
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
                    log.trace("description changed from {} to {}", srvDescription, description);
                    changed = true;
                }
                // else no change
                if((number != -1) && (srvNumber != number))
                {
                    log.trace("number changed from {} to {}", srvNumber, number);
                    changed = true;
                }
                // else no change
                if(true == changed)
                {
                    if(false == updateInterrupt(res.getInt(i, "id"), irqName, description, number))
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
            String param = "per_id=" + peripheralId + "&name=" + irqName + "&description=" + description + "&number=" + number;
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

    private boolean updateInterrupt(int int1, String irqName, String description, int number)
    {
        log.error("update interrupt not implemented!");
        return false;
    }

    private boolean checkAddressBlock(Response res, Element svdAaddressBlock)
    {
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

        boolean found = false;
        int numAddrBlockServer = res.numResults();
        for(int i = 0; i < numAddrBlockServer; i++)
        {
            int srvOffset = res.getInt(i, "address_offset");
            int srvSize = res.getInt(i, "size");
            String srvUsage = res.getString(i, "mem_usage");
            String srvProtection = res.getString(i, "protection");
            // check for changes
            if(    (offset != srvOffset)
                || (size != srvSize)
                || ((null != usage) && (false == usage.equals(srvUsage)))
                || ((null != protection) && (false == protection.equals(srvProtection)))
                )
            {
                /* This is not the Address Block we found in the SVD
                log.trace("offset : new: {} old : {}", offset, srvOffset);
                log.trace("size : new: {} old : {}", size, srvSize);
                log.trace("usage : new: {} old : {}", usage, srvUsage);
                log.trace("protection : new: {} old : {}", protection, srvProtection);
                */
            }
            else
            {
                found = true;
                break;
            }
        }
        if(false == found)
        {
            log.error("update addressblock not implemented!");
            return false;
        }
        else
        {
            return true;
        }
    }

    private boolean createPeripheralInstanceFrom(Element peripheral)
    {
        log.error("createPeripheralInstanceFrom not implemented!");
        return false;
    }

}
