package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.Tool;
import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdRegisterHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private final SvdFieldHandler fieldHandler;
    private int default_size = -1;
    private String default_access = null;
    private String default_resetValue = null;
    private String default_resetMask = null;

    public SvdRegisterHandler(Server srv)
    {
        this.srv = srv;
        fieldHandler = new SvdFieldHandler(srv);
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

    public boolean updateRegister(Element peripheral, int peripheralId)
    {
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
        return true;
    }

    public boolean updateDerivedRegister(Element svdDerivedPeripheral, Element svdOriginalPeripheral,
            int peripheralId)
    {
        Element registers = svdDerivedPeripheral.getChild("registers");
        if(null ==  registers)
        {
            registers = svdOriginalPeripheral.getChild("registers");
        }
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
        return true;
    }

    private boolean checkRegister(Response res, Element svdRegisters)
    {
        String name = null;
        String displayName = null;
        String description = null;
        String addressOffset = null;
        int size = default_size;
        String access = default_access;
        String reset_value = default_resetValue;
        String alternate_register = null;
        String reset_Mask = default_resetMask;
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
                String srvReset_Mask = res.getString(i, "reset_mask");
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

                if(   (null != reset_Mask)
                   && (false == "".equals(reset_Mask))
                   && (false == reset_Mask.equals(srvReset_Mask)))
                {
                    long val = Long.decode(reset_Mask);
                    long srvVal = 0;
                    if((null != srvReset_Mask) && (false == "".equals(srvReset_Mask)))
                    {
                        srvVal = Long.decode(srvReset_Mask);
                    }
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
                    return updateServerRegister(
                            srvId, // int id,
                            name, // String name,
                            displayName, // String display_name,
                            description, // String description,
                            addressOffset, // long address_offset,
                            size, // int size,
                            access, // String access,
                            reset_value, // String reset_value,
                            alternate_register, // String alternative_register,
                            reset_Mask, // String reset_mask,
                            read_action, // String read_action,
                            modified_write_values, // String modified_write_values,
                            data_type // String data_taype
                            );
                }
                // else no change -> no update needed
                break;
            }
        }

        if(false == found)
        {
            log.error("create new register not implemented!");
            return false;
        }
        else
        {
            if(null != fields)
            {
                if(false == fieldHandler.updateField(fields, srvId))
                {
                    return false;
                }
            }
            // else no fields in this register :-(
            return true;
        }
    }


    private boolean updateServerRegister(
            int id,
            String name,
            String display_name,
            String description,
            String address_offset,
            int size,
            String access,
            String reset_value,
            String alternative_register,
            String reset_mask,
            String read_action,
            String modified_write_values,
            String data_taype )
    {
        StringBuilder sb = new StringBuilder();
        sb.append("id=" + id);
        if(null != name)
        {
            sb.append("&name=" + name);
        }
        if(null != display_name)
        {
            sb.append("&display_name=" + display_name);
        }
        if(null != description)
        {
            sb.append("&description=" + description);
        }
        if(null != address_offset)
        {
            long val = Long.decode(address_offset);
            sb.append("&address_offset=" + val);
        }

        sb.append("&size=" + size);

        if(null != access)
        {
            sb.append("&access=" + access);
        }
        if(null != reset_value)
        {
            sb.append("&reset_value=" + reset_value);
        }
        if(null != alternative_register)
        {
            sb.append("&alternative_register=" + alternative_register);
        }
        if(null != reset_mask)
        {
            sb.append("&reset_mask=" + reset_mask);
        }
        if(null != read_action)
        {
            sb.append("&read_action=" + read_action);
        }
        if(null != modified_write_values)
        {
            sb.append("&modified_write_values=" + modified_write_values);
        }
        if(null != data_taype)
        {
            sb.append("&data_taype=" + data_taype);
        }
        String param = sb.toString();
        Response res = srv.put("register", param);

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
