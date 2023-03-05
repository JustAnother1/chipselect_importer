package org.chipselect.importer.parser;

import java.util.List;
import java.util.Vector;

import org.chipselect.importer.Tool;
import org.chipselect.importer.parser.svd.DimElementGroup;
import org.chipselect.importer.server.Request;
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

    private String svdName = null;
    private String description = null;
    private int bitOffset = -1;
    private int sizeBit = -1;
    private String access = null;
    private String modifiedWriteValues = null;
    private String readAction = null;
    private int dim = 0;
    private int dim_increment = 0;
    private String dim_index = null;
    private boolean isEnum = false;
    private String enumName = null;
    private String enumUsageRight = null;
    private Vector<Element> enum_values = new Vector<Element>();

    public SvdFieldHandler(Server srv)
    {
        this.srv = srv;
        enumHandler = new SvdEnumerationHandler(srv);
    }

    public boolean updateField(Element fields, int srvRegId)
    {
        if(0 == srvRegId)
        {
            log.error("Register ID invalid !");
            return false;
        }
        Request req = new Request("field", Request.GET);
        req.addPostParameter("reg_id", srvRegId);
        Response fieldstRes = srv.execute(req);
        if(false == fieldstRes.wasSuccessfull())
        {
            log.error("could not read the fields from the server");
            return false;
        }
        // else -> go on
        List<Element> fieldList = fields.getChildren();
        for(Element field : fieldList)
        {
            if(false == checkField(fieldstRes, field, srvRegId))
            {
                return false;
            }
        }
        return true;
    }

    private void initFieldValues()
    {
        svdName = null;
        description = null;
        bitOffset = -1;
        sizeBit = -1;
        access = null;
        modifiedWriteValues = null;
        readAction = null;
        dim = 0;
        dim_increment = 0;
        dim_index = null;
        isEnum = false;
        enumName = null;
        enumUsageRight = null;
        enum_values.clear();
    }

    private boolean checkForUnknownChildren(Element field)
    {
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
                svdName = Tool.cleanupString(child.getText());
                break;

            case "description":
                description = Tool.cleanupString(child.getText());
                break;

            case "bitOffset":
                bitOffset = (int)Tool.decode(child.getText());
                break;

            case "bitWidth":
                sizeBit= (int)Tool.decode(child.getText());
                break;

            case "lsb":
                bitOffset = (int)Tool.decode(child.getText());
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
                    sizeBit = (int)Tool.decode(child.getText());
                }
                else
                {
                    sizeBit = (int)Tool.decode(child.getText()) - bitOffset;
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
                bitOffset = (int)Tool.decode(parts[1]);
                sizeBit = (int)Tool.decode(parts[0]) + 1 - bitOffset;
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

            case "writeConstraint": // limits allowable write values, what if I write something else? does the chip explode?
            case "dimName":         // name of a C Structure to define the field
                // -> ignore for now.
                break;

            case "enumeratedValues":
                isEnum = true;
                if(false == parseEnumeratedValuesElement(child))
                {
                    return false;
                }
                break;

            case "dim":
                dim = (int)Tool.decode(child.getText());
                break;

            case "dimIncrement":
                dim_increment = (int)Tool.decode(child.getText());
                break;

            case "dimIndex":
                dim_index = child.getText();
                break;

            case "dimArrayIndex":
                log.error("field child dimArrayIndex (={}) not implemented!", child.getText());
                return false;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown field child tag: {}", name);
                return false;
            }
        }
        return true;
    }

    private boolean parseEnumeratedValuesElement(Element enumE)
    {
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
                enumName = Tool.cleanupString(child.getText());
                break;

            case "usage":
                enumUsageRight  = Tool.cleanupString(child.getText());
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

        return true;
    }

    private boolean checkIfUpdateOrNewField(Response res,  int reg_id)
    {
        log.info("checking field {}", svdName);
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
                String srvIsEnum = res.getString(i, "is_Enum");
                String srvEnumName = res.getString(i, "enum_name");
                String srvEnumUsageRight = res.getString(i, "enum_usage_right");
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
                if((true == isEnum) && (false == "1".equals(srvIsEnum)) )
                {
                    log.trace("is_Enum changed from :{}: to :1:", srvIsEnum, readAction);
                    changed = true;
                }
                // else no change
                if((false == isEnum) && (false == "0".equals(srvIsEnum)) )
                {
                    log.trace("is_Enum changed from :{}: to :0:", srvIsEnum, readAction);
                    changed = true;
                }
                // else no change
                if((null != enumName) && (false == "".equals(enumName)) && (false == enumName.equals(srvEnumName)))
                {
                    log.trace("read action changed from :{}: to :{}:", srvEnumName, enumName);
                    changed = true;
                }
                // else no change
                if((null != enumUsageRight) && (false == "".equals(enumUsageRight)) && (false == enumUsageRight.equals(srvEnumUsageRight)))
                {
                    log.trace("read action changed from :{}: to :{}:", srvEnumUsageRight, enumUsageRight);
                    changed = true;
                }
                // else no change

                if(true == changed)
                {
                    if(false == updateServerField(srvId,
                            svdName,
                            description,
                            bitOffset,
                            sizeBit,
                            access,
                            modifiedWriteValues,
                            readAction,
                            isEnum,
                            enumName,
                            enumUsageRight ))
                    {
                        log.error("failed to update field on server!");
                        return false;
                    }
                }
                // else no change -> no update needed
                break;
            }
        }
        if(false == found)
        {
            srvId = createNewFieldOnServer(
                    svdName,
                    description,
                    bitOffset,
                    sizeBit,
                    access,
                    modifiedWriteValues,
                    readAction,
                    isEnum,
                    enumName,
                    enumUsageRight,
                    reg_id );
            if(0 == srvId)
            {
                log.error("failed to create field on server!");
                return false;
            }
        }
        // field handled, -> enumeration values?
        if((true == isEnum) && (0 < enum_values.size() ) )
        {
            if(false == enumHandler.updateEnumeration(enum_values, srvId))
            {
                return false;
            }
            // else -> OK
        }
        // no enumeration values -> nothing to do
        return true;
    }

    private boolean checkField(Response res, Element field, int reg_id)
    {
        initFieldValues();
        if(false == checkForUnknownChildren(field))
        {
            return false;
        }

        // dim* ?
        if((0 != dim) || (0 != dim_increment))
        {
            // this is not one field but many,...
            DimElementGroup grp = new DimElementGroup(dim, dim_increment, dim_index);
            if(false == grp.isValid())
            {
                log.trace("\n" + Tool.getXMLRepresentationFor(field));
                log.error("invalid dim value");
                return false;
            }

            String groupName = svdName;
            int groupAddressOffsetBit = bitOffset;
            for(int i = 0; i< grp.getNumberElements(); i++)
            {
                // prepare values for this register
                // name
                svdName = grp.getElementNameFor(groupName, i);
                // addressOffsetBit
                bitOffset = groupAddressOffsetBit + (i * grp.getByteOffsetBytes());  // yes in this instance the bytes are bits!

                if(false == checkIfUpdateOrNewField(res, reg_id))
                {
                    return false;
                }
            }
        }
        else
        {
            if(false == checkIfUpdateOrNewField(res, reg_id))
            {
                return false;
            }
        }

        return true;
    }

    private boolean updateServerField(
            int id,
            String name,
            String description,
            int bit_offset,
            int size_bit,
            String access,
            String modified_write_values,
            String read_action,
            boolean isEnum,
            String enumName,
            String enumUsageRight)
    {
        Request req = new Request("field", Request.PUT);
        req.addPostParameter("id", id);
        if(null != name)
        {
            req.addPostParameter("name", name);
        }
        if(null != description)
        {
            req.addPostParameter("description", description);
        }
        req.addPostParameter("bit_offset", bit_offset);
        req.addPostParameter("size_bit", size_bit);
        if(null != access)
        {
            req.addPostParameter("access", access);
        }
        if(null != modified_write_values)
        {
            req.addPostParameter("modified_write_values", modified_write_values);
        }
        if(null != read_action)
        {
            req.addPostParameter("read_action", read_action);
        }
        if(false == isEnum)
        {
            req.addPostParameter("is_Enum", 0);
        }
        else
        {
            req.addPostParameter("is_Enum", 1);
        }
        if(null != enumName)
        {
            req.addPostParameter("enum_name", enumName);
        }
        if(null != enumUsageRight)
        {
            req.addPostParameter("enum_usage_right", enumUsageRight);
        }
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not update the field on the server");
            return false;
        }
        else
        {
            return true;
        }
    }

    private int createNewFieldOnServer(
            String name,
            String description,
            int bit_offset,
            int size_bit,
            String access,
            String modified_write_values,
            String read_action,
            boolean isEnum,
            String enumName,
            String enumUsageRight,
            int reg_id
            )
    {
        Request req = new Request("field", Request.POST);
        req.addPostParameter("name", name);
        if(null != description)
        {
            req.addPostParameter("description", description);
        }
        req.addPostParameter("bit_offset", bit_offset);
        req.addPostParameter("size_bit", size_bit);
        if(null != access)
        {
            req.addPostParameter("access", access);
        }
        if(null != modified_write_values)
        {
            req.addPostParameter("modified_write_values", modified_write_values);
        }
        if(null != read_action)
        {
            req.addPostParameter("read_action", read_action);
        }
        if(false == isEnum)
        {
            req.addPostParameter("is_Enum", 0);
        }
        else
        {
            req.addPostParameter("is_Enum", 1);
        }
        if(null != enumName)
        {
            req.addPostParameter("enum_name", enumName);
        }
        if(null != enumUsageRight)
        {
            req.addPostParameter("enum_usage_right", enumUsageRight);
        }
        req.addPostParameter("reg_id", reg_id);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not create a new field on the server");
            return 0;
        }
        else
        {
            return res.getInt("id");
        }
    }

}
