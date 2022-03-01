package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdAddressBlockHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private int default_size = -1;
    private String default_protection = null;

    public SvdAddressBlockHandler(Server srv)
    {
        this.srv = srv;
    }

    public void setDefaultSize(int default_size)
    {
        this.default_size = default_size;
    }

    public void setDefaultProtection(String default_protection)
    {
        this.default_protection = default_protection;
    }

    public boolean updateAddressBlock(Element svdReripheral, int srvPeripheralId)
    {
        Response AddrBlockRes = srv.get("address_block", "per_id=" + srvPeripheralId);
        if(false == AddrBlockRes.wasSuccessfull())
        {
            return false;
        }
        // else -> go on
        List<Element> AddrBlockchildren = svdReripheral.getChildren("addressBlock");
        for(Element addressBlock : AddrBlockchildren)
        {
            if(false == checkAddressBlock(AddrBlockRes, addressBlock))
            {
                return false;
            }
        }
        return true;
    }

    public boolean updateDerivedAddressBlock(Element svdDerivedPeripheral, Element svdOriginalPeripheral,
            int srvPeripheralId)
    {
        Response AddrBlockRes = srv.get("address_block", "per_id=" + srvPeripheralId);
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
            if(false == checkAddressBlock(AddrBlockRes, addressBlock))
            {
                return false;
            }
        }
        return true;
    }

    private boolean checkAddressBlock(Response res, Element svdAaddressBlock)
    {
        int offset = -1;
        int size = default_size;
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

}
