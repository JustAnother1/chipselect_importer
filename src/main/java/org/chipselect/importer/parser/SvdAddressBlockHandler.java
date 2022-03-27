package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.server.Request;
import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdAddressBlockHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private String default_size = null;
    private String default_protection = null;

    public SvdAddressBlockHandler(Server srv)
    {
        this.srv = srv;
    }

    public void setDefaultSize(String default_size)
    {
        this.default_size = default_size;
    }

    public void setDefaultProtection(String default_protection)
    {
        this.default_protection = default_protection;
    }

    public boolean updateAddressBlock(Element svdReripheral, int srvPeripheralId)
    {
        if(0 == srvPeripheralId)
        {
            log.error("Peripheral ID invalid !");
            return false;
        }
        Request req = new Request("address_block", Request.GET);
        req.addGetParameter("per_id", srvPeripheralId);
        Response AddrBlockRes = srv.execute(req);
        if(false == AddrBlockRes.wasSuccessfull())
        {
            return false;
        }
        // else -> go on
        List<Element> AddrBlockchildren = svdReripheral.getChildren("addressBlock");
        for(Element addressBlock : AddrBlockchildren)
        {
            if(false == checkAddressBlock(AddrBlockRes, addressBlock, srvPeripheralId))
            {
                return false;
            }
        }
        return true;
    }

    public boolean updateDerivedAddressBlock(Element svdDerivedPeripheral, Element svdOriginalPeripheral,
            int srvPeripheralId)
    {
        if(0 == srvPeripheralId)
        {
            log.error("Peripheral ID invalid !");
            return false;
        }
        Request req = new Request("address_block", Request.GET);
        req.addGetParameter("per_id", srvPeripheralId);
        Response AddrBlockRes = srv.execute(req);
        if(false == AddrBlockRes.wasSuccessfull())
        {
            return false;
        }
        // else -> go on
        List<Element> AddrBlockchildren = svdDerivedPeripheral.getChildren("addressBlock");
        if(true == AddrBlockchildren.isEmpty())
        {
            // no address block element in the derived peripheral
            // -> use the address blocks from the original peripheral
             AddrBlockchildren = svdOriginalPeripheral.getChildren("addressBlock");
        }
        // else the derived block overwrites the original block!
        for(Element addressBlock : AddrBlockchildren)
        {
            if(false == checkAddressBlock(AddrBlockRes, addressBlock, srvPeripheralId))
            {
                return false;
            }
        }
        return true;
    }

    private boolean checkAddressBlock(Response res, Element svdAaddressBlock, int srvPeripheralId)
    {
        HexString offset = null;
        HexString size = new HexString(default_size);
        String usage = null;
        String protection = default_protection;

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
                offset =  new HexString(child.getText());
                break;

            case "size":
                size = new HexString(child.getText());
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
        if(null == offset)
        {
            log.error("SVD does not specify the required address offset !");
            return false;
        }

        boolean found = false;
        int numAddrBlockServer = res.numResults();
        for(int i = 0; i < numAddrBlockServer; i++)
        {
            int srvId = res.getInt(i, "id");
            int srvOffset = res.getInt(i, "address_offset");
            String srvSize = res.getString(i, "size");
            String srvUsage = res.getString(i, "mem_usage");
            String srvProtection = res.getString(i, "protection");
            if((true == offset.equals(srvOffset)) && (0 != srvId))
            {
                // check for changes
                boolean changed = false;
                if(false ==size.equals(srvSize))
                {
                    if(null != size.toString())
                    {
                        log.trace("Size changed from {} to {} !", srvSize, size);
                        changed = true;
                    }
                }
                if((null != usage) && (false == usage.equals(srvUsage)))
                {
                    log.trace("usage changed from {} to {} !", srvUsage, usage);
                    changed = true;
                }
                if((null != protection) && (false == protection.equals(srvProtection)))
                {
                    log.trace("protection changed from {} to {} !", srvProtection, protection);
                    changed = true;
                }

                if(true == changed)
                {
                    if(null == srvProtection)
                    {
                        srvProtection = "";
                    }
                    if((null == protection) && (false == srvProtection.equals(default_protection)))
                    {
                        protection = default_protection;
                    }
                    if(false == updateAddressBlockToServer(
                            srvId,
                            offset.toString(), //address_offset,
                            size.toString(), // size,
                            usage, // mem_usage,
                            protection // protection,
                            ))
                    {
                        log.error("can not update the address block");
                        return false;
                    }
                }
                else
                {
                    found = true;
                    break;
                }
            }
            // else this is not the address block we are looking for -> keep looking
        }
        if(false == found)
        {
            if(null == protection)
            {
                protection = default_protection;
            }
            return postNewAddressBlockToServer(
                    offset.toString(), //address_offset,
                    size.toString(), // size,
                    usage, // mem_usage,
                    protection, // protection,
                    srvPeripheralId// peripheral_id
                    );
        }
        else
        {
            return true;
        }
    }

    private boolean updateAddressBlockToServer(
            int id,
            String address_offset,
            String size,
            String mem_usage,
            String protection)
    {
        Request req = new Request("address_block", Request.PUT);
        req.addGetParameter("id", id);
        if(null != address_offset)
        {
            req.addGetParameter("address_offset", address_offset);
        }
        if(null != size)
        {
            req.addGetParameter("size", size);
        }
        if(null != mem_usage)
        {
            req.addGetParameter("mem_usage", mem_usage);
        }
        if(null != protection)
        {
            req.addGetParameter("protection", protection);
        }
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

    private boolean postNewAddressBlockToServer(
            String address_offset,
            String size,
            String mem_usage,
            String protection,
            int peripheral_id)
    {
        Request req = new Request("address_block", Request.POST);
        if(null != address_offset)
        {
            req.addGetParameter("address_offset", address_offset);
        }
        if(null != size)
        {
            req.addGetParameter("size", size);
        }
        if(null != mem_usage)
        {
            req.addGetParameter("mem_usage", mem_usage);
        }
        if(null != protection)
        {
            req.addGetParameter("protection", protection);
        }
        req.addGetParameter("per_id", peripheral_id);
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
