package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.Tool;
import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdFieldHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private SvdEnumerationHandler enumHandler;

    public SvdFieldHandler(Server srv)
    {
        this.srv = srv;
        enumHandler = new SvdEnumerationHandler(srv);
    }

    public boolean updateField(Element fields, int srvId)
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
        return true;
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
                range = range.substring(1, range.length()); // remove // [
                range = range.substring(0, range.length() -1); // remove // ]
                String[] parts = range.split(":");
                if(parts.length != 2)
                {
                    log.error("Invalid Bit range definition of {} !", child.getText());
                    log.error("range: {}, parts: {} !", range, parts);
                    return false;
                }
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
                // check for Change
                boolean changed = false;
                if((null != description) && (false == "".equals(description)) && (false == description.equals(srvDescription)))
                {
                    log.trace("description changed from :{}: to :{}:", srvDescription, description);
                    changed = true;
                }
                // else no change
                if((bitOffset != -1) && (bitOffset != srvBitOffset))
                {
                    log.trace("bit Offset changed from :{}: to :{}:", srvBitOffset, bitOffset);
                    changed = true;
                }
                // else no change
                if((sizeBit != -1) && (sizeBit != srvSizeBit))
                {
                    log.trace("size_bit changed from :{}: to :{}:", srvSizeBit, sizeBit);
                    changed = true;
                }
                // else no change
                if((null != access) && (false == "".equals(access)) && (false == access.equals(srvAccess)))
                {
                    log.trace("access changed from :{}: to :{}:", srvAccess, access);
                    changed = true;
                }
                // else no change
                if((null != modifiedWriteValues) && (false == "".equals(modifiedWriteValues)) && (false == modifiedWriteValues.equals(srvModifiedWriteValues)))
                {
                    log.trace("modified write values changed from :{}: to :{}:", srvModifiedWriteValues, modifiedWriteValues);
                    changed = true;
                }
                // else no change
                if((null != readAction) && (false == "".equals(readAction)) && (false == readAction.equals(srvReadAction)))
                {
                    log.trace("read action changed from :{}: to :{}:", srvReadAction, readAction);
                    changed = true;
                }
                // else no change

                if(true == changed)
                {
                    return updateServerField(srvId,
                            svdName,
                            description,
                            bitOffset,
                            sizeBit,
                            access,
                            modifiedWriteValues,
                            readAction );
                }
                // else no change -> no update needed
                break;
            }
        }
        if(false == found)
        {
            // this field is missing on the server -> add it
            log.error("create new field not implemented!");
            return false;
        }
        else
        {
            // field handeled, -> enums?
            if(null != enumeration)
            {
                if(false == enumHandler.updateEnumeration(enumeration, srvId))
                {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean updateServerField(
            int id,
            String name,
            String description,
            int bit_offset,
            int size_bit,
            String access,
            String modified_write_values,
            String read_action )
    {
        StringBuilder sb = new StringBuilder();
        sb.append("id=" + id);
        if(null != name)
        {
            sb.append("&name=" + name);
        }
        if(null != description)
        {
            sb.append("&description=" + description);
        }

        sb.append("&bit_offset=" + bit_offset);
        sb.append("&size_bit=" + size_bit);

        if(null != access)
        {
            sb.append("&access=" + access);
        }
        if(null != modified_write_values)
        {
            sb.append("&modified_write_values=" + modified_write_values);
        }
        if(null != read_action)
        {
            sb.append("&read_action=" + read_action);
        }

        String param = sb.toString();
        Response res = srv.put("field", param);

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
