package org.chipselect.importer.parser;

import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemViewDescription
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private String specified_vendor_name = null;
    private int vendor_id = 0;
    private String device_name = null;
    private int device_id = 0;

    public SystemViewDescription(Server chipselect)
    {
        srv = chipselect;
    }


    private boolean handleVendor(Element device)
    {
        // get Vendor
        String vendorName = null;
        Element vendor = device.getChild("vendor");
        if(null == vendor)
        {
            if(null == specified_vendor_name)
            {
                log.error("No vendor name provided! not in SVD, not as parameter!");
                return false;
            }
            else
            {
                log.warn("no Vendor name in SVD !");
                vendorName = specified_vendor_name;
            }
        }
        else
        {
            vendorName = vendor.getText();
            log.info("Vendor from SVD : {}", vendorName);
        }
        Response res = srv.get("vendor", "name=" + vendorName);
        int id = res.getInt("id");
        if(0 == id)
        {
            // this vendor is not on the server
            log.info("The Vendor {} is not known on the server !", vendorName);
            Response post_res = srv.post("vendor", "name=" + vendorName);
            int new_id = post_res.getInt("id");
            if(0 == new_id)
            {
                log.error("Failed to create new vendor!");
                return false;
            }
            else
            {
                vendor_id = new_id;
                return true;
            }
        }
        else
        {
            vendor_id = id;
            return true;
        }
    }

    private boolean handleName(Element device)
    {
        // get Vendor
        Element Name = device.getChild("name");
        if(null == Name)
        {
            log.error("The device name is missing in the SVD !");
            return false;
        }

        device_name = Name.getText();
        log.trace("device name from SVD : {}", device_name);
        Response res = srv.get("microcontroller", "name=" + device_name);
        int id = res.getInt("id");
        if(0 == id)
        {
            // this device is not on the server
            // TODO create the device
            log.error("The device {} is not known on the server !", device_name);
            return false;
        }
        else
        {
            device_id = id;
        }
        // TODO
    }

    public boolean parse(Document doc)
    {
        // is valid XML?
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
        if(false == "device".equals(device.getName()))
        {
            log.error("XML root element is {} (expected:device) !", device.getName());
            return false;
        }
        if(false == handleVendor(device))
        {
            return false;
        }
        if(false == handleName(device))
        {
            return false;
        }


        // TODO
        return true;
    }

    public void setVendorName(String vendor_name)
    {
        specified_vendor_name = vendor_name;
    }



}
