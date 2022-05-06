package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.server.Request;
import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeggerXmlParser
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;

    public SeggerXmlParser(Server chipselect)
    {
        this.srv = chipselect;
    }

    public boolean parse(Document doc)
    {
        if(null == doc)
        {
            log.error("XML Document is NULL !");
            return false;
        }
        Element device = doc.getRootElement();
        if(null == device)
        {
            log.error("XML root element is NULL!");
            return false;
        }
        if(false == "DeviceDatabase".equals(device.getName()))
        {
            log.error("XML root element is {} (expected:DeviceDatabase) !", device.getName());
            return false;
        }

        List<Element> children = device.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            if(false == "VendorInfo".equals(name))
            {
                log.error("unexpected XML element {} on top level (VendorInfo) !", name);
                return false;
            }
            String vendorName = child.getAttributeValue("Name");
            log.trace("Found Vendor {}", vendorName);
            if(true == "Unspecified".equals(vendorName))
            {
                // the ""Unspecified" Vendor is different
                if(false == handleUnsepcifiedVendor(child))
                {
                    return false;
                }
            }
            else
            {
                if(false == handleVendor(child))
                {
                    return false;
                }
            }
        }
        // parsed everything!
        return true;
    }

    private boolean handleVendor(Element vendorElement)
    {
        String vendorName = vendorElement.getAttributeValue("Name");
        Request req = new Request("vendor", Request.GET);
        req.addGetParameter("name", vendorName);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not read the vendor from the server");
            return false;
        }
        else
        {
            log.trace("Vendor {} is available on the server", vendorName);
        }
        int id = res.getInt("alternative");
        if(id == 0)
        {
            id = res.getInt("id");
        }
        if(id == 0)
        {
            log.error("no id for vendor from server");
            return false;
        }
        // DeviceInfo
        List<Element> children = vendorElement.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            if(false == "DeviceInfo".equals(name))
            {
                log.error("unexpected XML element {} on vendor level (DeviceInfo) !", name);
                return false;
            }
            if(false == handleDevice(id, child))
            {
                return false;
            }
        }
        return true;
    }

    private int getServerDeviceId(String device_name)
    {
        Request req = new Request("microcontroller", Request.GET);
        req.addGetParameter("name", device_name);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not read the device from the server");
            return 0;
        }
        int id = res.getInt("id");
        return id;
    }

    private boolean handleDevice(int id, Element deviceElement)
    {
        String deviceName = deviceElement.getAttributeValue("Name");
        String coreName = deviceElement.getAttributeValue("Core");
        String ramStart = deviceElement.getAttributeValue("WorkRAMStartAddr");
        String ramSize = deviceElement.getAttributeValue("WorkRAMSize");
        log.trace("Device {} is a {}", deviceName, coreName);
        log.trace("RAM is @{} of size {}", ramStart, ramSize);
        if(0 == getServerDeviceId(deviceName))
        {
            log.info("Device is not on the server!");
        }
        // TODO
        List<Element> children = deviceElement.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            case "FlashBankInfo":
                // TODO
                break;

            case "AliasInfo":
                String deviceAliasName = child.getAttributeValue("Name");
                if(0 == getServerDeviceId(deviceAliasName))
                {
                    log.info("Device({}) is not on the server!", deviceAliasName);
                }
                // TODO
                break;

            default:
                log.error("unexpected XML element {} on device level !", name);
                return false;
            }
        }
        return true;
    }

    private boolean handleUnsepcifiedVendor(Element architecturesElement)
    {
        // DeviceInfo
        List<Element> children = architecturesElement.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            if(false == "DeviceInfo".equals(name))
            {
                log.error("unexpected XML element {} on architecture level (DeviceInfo) !", name);
                return false;
            }
            if(false == handleArchitecture(child))
            {
                return false;
            }
        }

        return true;
    }

    private boolean handleArchitecture(Element architectureElement)
    {
        String Name = architectureElement.getAttributeValue("Name"); // "Core" attribute has the same value
        int ArchitectureId = 0;
        List<Element> children = architectureElement.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            if(false == "AliasInfo".equals(name))
            {
                log.error("unexpected XML element {} on architecture level (AliasInfo) !", name);
                return false;
            }
            if(0 == ArchitectureId)
            {
                // this is the first Child of this architecture -> get ID from server
                Request req = new Request("architecture", Request.GET);
                req.addGetParameter("name", Name);
                Response res = srv.execute(req);
                if(false == res.wasSuccessfull())
                {
                    log.error("could not read the architecture from the server");
                    return false;
                }
                else
                {
                    log.trace("architecture {} is available on the server", Name);
                    ArchitectureId = res.getInt("id");
                }
            }
            //TODO add Device
            return false;
        }
        return true;
    }

}
