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

public class SvdRegisterHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private final SvdFieldHandler fieldHandler;
    private int default_size = -1;
    private String default_access = null;
    private String default_resetValue = null;
    private String default_resetMask = null;
    // register values
    private String name = null;
    private String displayName = null;
    private String description = null;
    private HexString addressOffset = new HexString(0);
    private long size = 0;
    private String access = null;
    private HexString reset_value = new HexString(0);
    private String alternate_register = null;
    private String alternate_group = null;
    private HexString reset_Mask =new HexString(0);
    private String read_action = null;
    private String modified_write_values = null;
    private String data_type = null;
    private Element fields = null;
    private int dim = 0;
    private int dim_increment = 0;
    private String dim_index = null;

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
        if(0 == peripheralId)
        {
            log.error("Peripheral ID invalid !");
            return false;
        }
        Element registers = peripheral.getChild("registers");
        if(null !=  registers)
        {
            Request req = new Request("register", Request.GET);
            req.addPostParameter("per_id", peripheralId);
            Response res = srv.execute(req);
            if(false == res.wasSuccessfull())
            {
                log.error("could not read the registers from the server");
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
                    if(false == checkCluster(res, child, peripheralId))
                    {
                        return false;
                    }
                    break;

                case "register":
                    if(false == checkRegister(res, child, peripheralId))
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
        if(0 == peripheralId)
        {
            log.error("Peripheral ID invalid !");
            return false;
        }
        Element registers = svdDerivedPeripheral.getChild("registers");
        if(null ==  registers)
        {
            registers = svdOriginalPeripheral.getChild("registers");
        }
        if(null !=  registers)
        {
            Request req = new Request("register", Request.GET);
            req.addPostParameter("per_id", peripheralId);
            Response res = srv.execute(req);
            if(false == res.wasSuccessfull())
            {
                log.error("could not read the registers from the server");
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
                    if(false == checkCluster(res, child, peripheralId))
                    {
                        return false;
                    }
                    break;

                case "register":
                    if(false == checkRegister(res, child, peripheralId))
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

    private boolean checkCluster(Response res, Element cluster, int peripheralId)
    {
        String clusterName = null;
        String clusterDescription = null;
        HexString clusterAddressOffset =  new HexString(0);
        long clusterSize = 0;
        String clusterAccess = null;
        HexString clusterResetValue =  new HexString(0);
        HexString clusterResetMask =  new HexString(0);
        Vector<Element> registers = new Vector<Element>();
        Vector<Element> clusters = new Vector<Element>();
        int clusterDim = 0;
        int clusterDimIncrement = 0;
        String clusterDimIndex = null;

        String derived = cluster.getAttributeValue("derivedFrom");
        if(null != derived)
        {
            log.error(Tool.getXMLRepresentationFor(cluster));
            log.error("Derived Clusters not implemented !");
            return false;
        }
        List<Element> children = cluster.getChildren();
        for(Element child : children)
        {
            String tagName = child.getName();
            switch(tagName)
            {
            // all defined child types from SVD standard

            case "dim" :
                clusterDim = (int)Tool.decode(child.getText());
                break;

            case "dimIncrement":
                clusterDimIncrement = (int)Tool.decode(child.getText());
                break;

            case "dimIndex" :
                clusterDimIndex = child.getText();
                if(null != clusterDimIndex)
                {
                    clusterDimIndex = clusterDimIndex.trim();
                }
                break;

            case "dimName" :
            case "dimArrayIndex" :
                // ignoring this for now.
                break;

            case "name" :
                clusterName = child.getText();
                if(null != clusterName)
                {
                    clusterName = clusterName.trim();
                }
                break;

            case "description" :
                clusterDescription = Tool.cleanupString(child.getText());
                break;

            case "alternateCluster" :
            case "headerStructName":
                // we ignore this for now.
                break;

            case "addressOffset" :
                clusterAddressOffset = new HexString(child.getText());
                break;

            case "size" :
                clusterSize = Tool.decode(child.getText());
                break;

            case "access" :
                clusterAccess = child.getText();
                if(null != clusterAccess)
                {
                    clusterAccess = clusterAccess.trim();
                }
                break;

            case "protection" :
                // we ignore that for now
                break;

            case "resetValue" :
                clusterResetValue = new HexString(child.getText());
                break;

            case "resetMask" :
                clusterResetMask = new HexString(child.getText());
                break;

            case "register" :
                registers.add(child);
                break;

            case "cluster" :
                clusters.add(child);
                break;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown cluster child tag: {}", tagName);
                return false;
            }
        }
        // dim* ?
        if((0 != clusterDim) || (0 != clusterDimIncrement))
        {
            DimElementGroup cluGrp = new DimElementGroup(clusterDim, clusterDimIncrement, clusterDimIndex);
            if(false == cluGrp.isValid())
            {
                log.trace("\n" + Tool.getXMLRepresentationFor(cluster));
                log.error("invalid dim value");
                return false;
            }

            for(int ci = 0; ci< cluGrp.getNumberElements(); ci++)
            {
                for(int ri = 0; ri < registers.size(); ri++)
                {
                    Element child = registers.elementAt(ri);
                    // prepare values for this register
                    // make sure that all values are fresh and clean
                    initRegisterValues();
                    name = cluGrp.getElementNameFor(clusterName, ci);
                    description = clusterDescription;
                    addressOffset = clusterAddressOffset.add(ci * cluGrp.getByteOffsetBytes());
                    size = clusterSize;
                    access = clusterAccess;
                    reset_value = clusterResetValue;
                    reset_Mask = clusterResetMask;

                    // check for unknown children
                    if(false == checkRegisterForUnknownChildren(child))
                    {
                        return false;
                    }
                    // that check also populated the values of the registers into the member variables.

                    // dim* ?
                    if((0 != dim) || (0 != dim_increment))
                    {
                        // this is not one register but many,...
                        DimElementGroup grp = new DimElementGroup(dim, dim_increment, dim_index);
                        if(false == grp.isValid())
                        {
                            log.trace("\n" + Tool.getXMLRepresentationFor(child));
                            log.error("invalid dim value");
                            return false;
                        }
                        String groupName = name;
                        String groupDisplayName = displayName;
                        Long groupAddressOffset = Tool.decode(addressOffset.toString());
                        for(int i = 0; i< grp.getNumberElements(); i++)
                        {
                            // prepare values for this register
                            // name
                            name = grp.getElementNameFor(groupName, i);
                            // displayName
                            displayName = grp.getElementNameFor(groupDisplayName, i);
                            // addressOffset
                            Long addressOffsetLong = groupAddressOffset + (i * grp.getByteOffsetBytes());
                            HexString DimAddressOffset = new HexString(addressOffsetLong);
                            if(false == checkIfUpdateOrNewRegister(res, peripheralId, DimAddressOffset))
                            {
                                return false;
                            }
                        }
                    }
                    else
                    {
                        if(false == checkIfUpdateOrNewRegister(res, peripheralId, addressOffset))
                        {
                            return false;
                        }
                    }
                }
                for(int i = 0; i < clusters.size(); i++)
                {
                    Element child = clusters.elementAt(i);
                    if(false == checkCluster(res, child, peripheralId))
                    {
                        return false;
                    }
                }
            }
        }
        else
        {
            if((0 < registers.size()) || (0 < clusters.size()))
            {
                // in violation of the standard someone just wanted to group some registers.
                // so ignore the cluster and just add the registers
                for(int ri = 0; ri < registers.size(); ri++)
                {
                    Element child = registers.elementAt(ri);
                    if(false == checkRegister(res, child, peripheralId))
                    {
                        return false;
                    }
                }
                for(int ci = 0; ci < clusters.size(); ci++)
                {
                    Element child = clusters.elementAt(ci);
                    if(false == checkCluster(res, child, peripheralId))
                    {
                        return false;
                    }
                }
            }
            else
            {
                log.error("invalid cluster definition, no dim!(dim:{}, dim increment:{})", clusterDim, clusterDimIncrement);
                log.error("\n" + Tool.getXMLRepresentationFor(cluster));
                return false;
            }
        }
        return true;
    }

    private boolean checkRegisterForUnknownChildren(Element svdRegister)
    {
        List<Element> children = svdRegister.getChildren();
        for(Element child : children)
        {
            String tagName = child.getName();
            switch(tagName)
            {
            // all defined child types from SVD standard
            case "name" :
                name = child.getText();
                if(null != name)
                {
                    name = name.trim();
                }
                break;

            case "displayName" :
                displayName = Tool.cleanupString(child.getText());
                break;

            case "description" :
                description = Tool.cleanupString(child.getText());
                break;

            case "alternateRegister" :
                alternate_register = child.getText();
                if(null != alternate_register)
                {
                    alternate_register = alternate_register.trim();
                }
                break;

            case "addressOffset" :
                addressOffset = new HexString(child.getText());
                break;

            case "size" :
                size = Tool.decode(child.getText());
                break;

            case "access" :
                access = child.getText();
                if(null != access)
                {
                    access = access.trim();
                }
                break;

            case "protection" :
                // we ignore this for now
                break;

            case "resetValue" :
                reset_value = new HexString(child.getText());
                break;

            case "resetMask" :
                reset_Mask = new HexString(child.getText());
                break;

            case "dataType" :
                data_type = child.getText();
                if(null != data_type)
                {
                    data_type = data_type.trim();
                }
                break;

            case "modifiedWriteValues" :
                modified_write_values = child.getText();
                if(null != modified_write_values)
                {
                    modified_write_values = modified_write_values.trim();
                }
                break;

            case "readAction" :
                read_action = child.getText();
                if(null != read_action)
                {
                    read_action = read_action.trim();
                }
                break;

            case "fields" :
                fields = child;
                break;

            case "dim" :
                dim = (int)Tool.decode(child.getText());
                break;

            case "dimIncrement":
                dim_increment = (int)Tool.decode(child.getText());
                break;

            case "dimIndex" :
                dim_index = child.getText();
                if(null != dim_index)
                {
                    dim_index = dim_index.trim();
                }
                break;

            case "alternateGroup" :
                alternate_group = child.getText();
                if(null != alternate_group)
                {
                    alternate_group = alternate_group.trim();
                }
                break;

            case "dimName" :
            case "dimArrayIndex" :
            case "writeConstraint" :
                log.error("Register child {} (={}) not implemented!", tagName, child.getText());
                log.error("\n" + Tool.getXMLRepresentationFor(svdRegister));
                return false;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown register child tag: {}", tagName);
                log.error("\n" + Tool.getXMLRepresentationFor(svdRegister));
                return false;
            }
        }
        return true;
    }

    private void initRegisterValues()
    {
        name = null;
        displayName = null;
        description = null;
        size = default_size;
        access = default_access;
        addressOffset = null;
        reset_value = new HexString(default_resetValue);
        alternate_register = null;
        alternate_group = null;
        reset_Mask = new HexString(default_resetMask);
        read_action = null;
        modified_write_values = null;
        data_type = null;
        fields = null;
        dim = 0;
        dim_increment = 0;
        dim_index = null;
    }

    private boolean checkIfUpdateOfRegisterIsNeeded(Response res, int i, int srvId, HexString localAddressOffset)
    {
        String srvDisplayName = res.getString(i, "display_name");
        String srvDescription = res.getString(i, "description");
        String srvAddressOffsetVal = res.getString(i, "address_offset");
        long   srvSize = res.getInt(i,  "size");
        String srvAccess = res.getString(i, "access");
        String srvReset_value = res.getString(i, "reset_value");
        String srvAlternate_register = res.getString(i, "alternate_register");
        String srvAlternate_group = res.getString(i, "alternate_group");
        String srvReset_Mask = res.getString(i, "reset_mask");
        String srvRead_action = res.getString(i, "read_action");
        String srvModified_write_values = res.getString(i, "modified_write_values");
        String srvData_type = res.getString(i, "data_type");

        // check for Change
        boolean changed = false;
        if((null != displayName) && (false == "".equals(displayName)) && (false == displayName.equals(srvDisplayName)))
        {
            log.trace("display name changed from :{}: to :{}:", srvDisplayName, displayName);
            changed = true;
        }
        // else no change
        if((null != description) && (false == "".equals(description)) && (false == description.equals(srvDescription)))
        {
            log.trace("description changed from :{}: to :{}:", srvDescription, description);
            changed = true;
        }
        // else no change

        if(null != localAddressOffset)
        {
            if(false == localAddressOffset.equals(srvAddressOffsetVal))
            {
                if(null != localAddressOffset.toString())
                {
                    log.trace("address offset changed from :{}: to :{}:", srvAddressOffsetVal, localAddressOffset);
                    changed = true;
                }
            }
        }
        // else no change

        // else no change
        if((size != -1) && (size != srvSize))
        {
            log.trace("size changed from :{}: to :{}:", srvSize, size);
            changed = true;
        }
        // else no change
        if((null != access) && (false == "".equals(access)) && (false == access.equals(srvAccess)))
        {
            log.trace("access changed from :{}: to :{}:", srvAccess, access);
            changed = true;
        }
        // else no change
        if(null != reset_value)
        {
            if(false == reset_value.equals(srvReset_value))
            {
                if(null != reset_value.toString())
                {
                    log.trace("reset value changed from :{}: to :{}:", srvReset_value, reset_value);
                    changed = true;
                }
            }

            // else "0" is not different to "0x00"
        }
        // else no change

        if((null != alternate_register) && (false == "".equals(alternate_register)) && (false == alternate_register.equals(srvAlternate_register)))
        {
            log.trace("alternate register changed from :{}: to :{}:", srvAlternate_register, alternate_register);
            changed = true;
        }
        // else no change

        if((null != alternate_group) && (false == "".equals(alternate_group)) && (false == alternate_group.equals(srvAlternate_group)))
        {
            log.trace("alternate group changed from :{}: to :{}:", srvAlternate_group, alternate_group);
            changed = true;
        }
        // else no change

        if(null != reset_Mask)
        {
            if(false == reset_Mask.equals(srvReset_Mask))
            {
                if(null != reset_Mask.toString())
                {
                    log.trace("reset mask changed from :{}: to :{}:", srvReset_Mask, reset_Mask);
                    changed = true;
                }
            }
            // else "0" is not different to "0x00"
        }
        // else no change

        if((null != read_action) && (false == "".equals(read_action)) && (false == read_action.equals(srvRead_action)))
        {
            log.trace("read action changed from :{}: to :{}:", srvRead_action, read_action);
            changed = true;
        }
        // else no change
        if((null != modified_write_values) && (false == "".equals(modified_write_values)) && (false == modified_write_values.equals(srvModified_write_values)))
        {
            log.trace("modified write values changed from :{}: to :{}:", srvModified_write_values, modified_write_values);
            changed = true;
        }
        // else no change
        if((null != data_type) && (false == "".equals(data_type)) && (false == data_type.equals(srvData_type)))
        {
            log.trace("data type changed from :{}: to :{}:", srvData_type, data_type);
            changed = true;
        }
        // else no change

        if(true == changed)
        {
            if(false == updateServerRegister(
                    srvId, // id,
                    name, // name,
                    displayName, // display_name,
                    description, // description,
                    localAddressOffset.toString(), // address_offset,
                    size, // size,
                    access, // access,
                    reset_value.toString(), // reset_value,
                    alternate_register, // alternative_register,
                    reset_Mask.toString(), // reset_mask,
                    read_action, // read_action,
                    modified_write_values, // modified_write_values,
                    data_type, // data_type
                    alternate_group // alternate_group
                    ))
            {
                log.error("Failed to update register on server");
                return false;
            }
        }
        // else no change -> no update needed
        return true;
    }


    private boolean checkIfUpdateOrNewRegister(Response res, int peripheralId, HexString localAddressOffset)
    {
        int srvId = -1;
        if(null == name)
        {
            log.error("Register does not have a name !");
            return false;
        }
        log.trace("checking register {}", name);

        boolean found = false;
        int numRegisterServer = res.numResults();
        for(int i = 0; i < numRegisterServer; i++)
        {
            String srvName = res.getString(i, "name");
            if(null == srvName)
            {
                continue;
            }
            else
            {
                srvName = srvName.trim();
                if(true == name.equals(srvName))
                {
                    found = true;
                    srvId = res.getInt(i,  "id");
                    log.trace("found register {} ({})", name, srvId);
                    if(false == checkIfUpdateOfRegisterIsNeeded(res, i, srvId, localAddressOffset))
                    {
                        return false;
                    }
                    break;
                }
            }
        }

        if(false == found)
        {
            srvId = createRegisterOnServer(
                    name, // name,
                    displayName, // display_name,
                    description, // description,
                    localAddressOffset.toString(), // address_offset,
                    size, // size,
                    access, // access,
                    reset_value.toString(), // reset_value,
                    alternate_register, // alternative_register,
                    reset_Mask.toString(), // reset_mask,
                    read_action, // read_action,
                    modified_write_values, // modified_write_values,
                    data_type, // data_taype
                    alternate_group, // alternate_group
                    peripheralId);
            if(0 == srvId)
            {
                log.error("Failed to create register on server !");
                return false;
            }
        }
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


    private boolean checkRegister(Response res, Element svdRegister, int peripheralId)
    {
        // make sure that all values are fresh and clean
        initRegisterValues();
        // check for unknown children
        if(false == checkRegisterForUnknownChildren(svdRegister))
        {
            return false;
        }
        // that check also populated the values of the registers into the member variables.

        // dim* ?
        if((0 != dim) || (0 != dim_increment))
        {
            // this is not one register but many,...
            DimElementGroup grp = new DimElementGroup(dim, dim_increment, dim_index);
            if(false == grp.isValid())
            {
                log.trace("\n" + Tool.getXMLRepresentationFor(svdRegister));
                log.error("invalid dim value");
                return false;
            }
            String groupName = name;
            String groupDisplayName = displayName;
            Long groupAddressOffset = Tool.decode(addressOffset.toString());
            for(int i = 0; i< grp.getNumberElements(); i++)
            {
                // prepare values for this register
                // name
                name = grp.getElementNameFor(groupName, i);
                if(null != name)
                {
                    name = name.trim();
                }
                // displayName
                displayName = grp.getElementNameFor(groupDisplayName, i);
                // addressOffset
                Long addressOffsetLong = groupAddressOffset + (i * grp.getByteOffsetBytes());
                HexString DimAddressOffset = new HexString(addressOffsetLong);
                if(false == checkIfUpdateOrNewRegister(res, peripheralId, DimAddressOffset))
                {
                    return false;
                }
            }
        }
        else
        {
            if(false == checkIfUpdateOrNewRegister(res, peripheralId, addressOffset))
            {
                return false;
            }
        }
        return true;
    }

    private boolean updateServerRegister(
            int    id,
            String name,
            String display_name,
            String description,
            String address_offset,
            long   size,
            String access,
            String reset_value,
            String alternative_register,
            String reset_mask,
            String read_action,
            String modified_write_values,
            String data_type,
            String alternate_group)
    {
        Request req = new Request("register", Request.PUT);
        req.addPostParameter("id", id);
        if(null != name)
        {
            req.addPostParameter("name", name);
        }
        if(null != display_name)
        {
            req.addPostParameter("display_name", display_name);
        }
        if(null != description)
        {
            req.addPostParameter("description", description);
        }
        if(null != address_offset)
        {
            req.addPostParameter("address_offset", address_offset);
        }
        req.addPostParameter("size", size);
        if(null != access)
        {
            req.addPostParameter("access", access);
        }
        if(null != reset_value)
        {
            req.addPostParameter("reset_value", reset_value);
        }
        if(null != alternative_register)
        {
            req.addPostParameter("alternate_register", alternative_register);
        }
        if(null != reset_mask)
        {
            req.addPostParameter("reset_mask", reset_mask);
        }
        if(null != read_action)
        {
            req.addPostParameter("read_action", read_action);
        }
        if(null != modified_write_values)
        {
            req.addPostParameter("modified_write_values", modified_write_values);
        }
        if(null != data_type)
        {
            req.addPostParameter("data_type", data_type);
        }
        if(null != alternate_group)
        {
            req.addPostParameter("alternate_group", alternate_group);
        }
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not update the register on the server");
            return false;
        }
        else
        {
            return true;
        }
    }

    private int createRegisterOnServer(
            String name,
            String display_name,
            String description,
            String address_offset,
            long   size,
            String access,
            String reset_value,
            String alternative_register,
            String reset_mask,
            String read_action,
            String modified_write_values,
            String data_type,
            String alternate_group,
            int peripheralId)
    {
        Request req = new Request("register", Request.POST);
        req.addPostParameter("name", name);
        if(null != display_name)
        {
            req.addPostParameter("display_name", display_name);
        }
        if(null != description)
        {
            req.addPostParameter("description", description);
        }
        if(null != address_offset)
        {
            req.addPostParameter("address_offset", address_offset);
        }
        req.addPostParameter("size", size);
        if(null != access)
        {
            req.addPostParameter("access", access);
        }
        if(null != reset_value)
        {
            req.addPostParameter("reset_value", reset_value);
        }
        if(null != alternative_register)
        {
            req.addPostParameter("alternative_register", alternative_register);
        }
        if(null != reset_mask)
        {
            req.addPostParameter("reset_mask", reset_mask);
        }
        if(null != read_action)
        {
            req.addPostParameter("read_action", read_action);
        }
        if(null != modified_write_values)
        {
            req.addPostParameter("modified_write_values", modified_write_values);
        }
        if(null != data_type)
        {
            req.addPostParameter("data_type", data_type);
        }
        if(null != alternate_group)
        {
            req.addPostParameter("alternate_group", alternate_group);
        }
        req.addPostParameter("per_id", peripheralId);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not create the new register on the server");
            return 0;
        }
        else
        {
            return res.getInt("id");
        }
    }

}
