package org.chipselect.importer.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Attribute;
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
    private Response device_response = null;

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
        if(false == res.wasSuccessfull())
        {
            return false;
        }
        int id = 0;
        int alt = res.getInt("alternative");
        if(alt != 0)
        {
            id = alt;
        }
        else
        {
            id = res.getInt("id");
        }
        if(0 == id)
        {
            // this vendor is not on the server
            log.info("The Vendor {} is not known on the server !", vendorName);
            Response post_res = srv.post("vendor", "name=" + vendorName);
            if(false == post_res.wasSuccessfull())
            {
                return false;
            }
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
        // Vendor must already be known!
        if(0 == vendor_id)
        {
            log.error("Vendor must already be known!");
            return false;
        }
        // get Name
        Element Name = device.getChild("name");
        if(null == Name)
        {
            log.error("The device name is missing in the SVD !");
            return false;
        }

        device_name = Name.getText();
        log.trace("device name from SVD : {}", device_name);
        Response res = srv.get("microcontroller", "name=" + device_name);
        if(false == res.wasSuccessfull())
        {
            return false;
        }
        int id = res.getInt("id");
        if(0 == id)
        {
            // this device is not on the server
            log.info("The device {} is not known on the server !", device_name);
            Response post_res = srv.post("microcontroller", "name=" + device_name + "&vendor_id=" + vendor_id);
            if(false == post_res.wasSuccessfull())
            {
                return false;
            }
            int new_id = post_res.getInt("id");
            if(0 == new_id)
            {
                log.error("Failed to create new device!");
                return false;
            }
            else
            {
                device_id = new_id;
                res = srv.get("microcontroller", "name=" + device_name);
                if(false == res.wasSuccessfull())
                {
                    return false;
                }
                device_response = res;
            }
        }
        else
        {
            device_id = id;
            device_response = res;
        }
        return true;
    }

    private boolean handleCpuElement(Element cpuElement, int srvArchitectureId)
    {
        if(null == cpuElement)
        {
            // CPU element is optional
            return true;
        }
        String svdName = cpuElement.getChildText("name");
        String svdRevision = cpuElement.getChildText("revision");
        String svdEndian = cpuElement.getChildText("endian");
        String svdNvicPrioBits = cpuElement.getChildText("nvicPrioBits");
        String svdVendorSystickConfig = cpuElement.getChildText("vendorSystickConfig");
        String svdMpuPresent = cpuElement.getChildText("mpuPresent");
        // from svd standard:  This tag is either set to true or false, 1 or 0.
        int svdMpuPresentInt = 0;
        if("true".equals(svdMpuPresent))
        {
            svdMpuPresentInt = 1;
        }
        else if("false".equals(svdMpuPresent))
        {
            svdMpuPresentInt = 0;
        }
        else
        {
            svdMpuPresentInt = Integer.valueOf(svdMpuPresent);
        }
        // from svd standard:  This tag is either set to true or false, 1 or 0.
        String svdFpuPresent = cpuElement.getChildText("fpuPresent");
        int svdFpuPresentInt = 0;
        if("true".equals(svdFpuPresent))
        {
            svdFpuPresentInt = 1;
        }
        else if("false".equals(svdFpuPresent))
        {
            svdFpuPresentInt = 0;
        }
        else
        {
            svdFpuPresentInt = Integer.valueOf(svdFpuPresent);
        }
        // ignoring optional tags:
        // fpuDP
        // dspPresent
        // icachePresent
        // dcachePresent
        // itcmPresent
        // dtcmPresent
        // vtorPresent
        // deviceNumInterrupts
        // sauNumRegions
        // sauRegionsConfig
        if((null == svdName) || (null == svdRevision) || (null == svdEndian) || (null == svdNvicPrioBits) || (null == svdVendorSystickConfig))
        {
            return false;
        }
        if((1 > svdName.length()) || (1 > svdRevision.length()) || (1 > svdEndian.length()) || (1 > svdNvicPrioBits.length()) || (1 > svdVendorSystickConfig.length()))
        {
            return false;
        }
        log.trace("Name: {}", svdName);
        log.trace("Revision: {}", svdRevision);
        log.trace("Endian: {}", svdEndian);
        log.trace("NVIC Priority Bits: {}", svdNvicPrioBits);
        int svdNvicPrioBitsInt = Integer.valueOf(svdNvicPrioBits);
        log.trace("NVIC Priority Bits(int): {}", svdNvicPrioBitsInt);
        log.trace("Vendor Systick Configuration: {}", svdVendorSystickConfig);
        int svdVendorSystickConfigInt = 0;
        if("true".equals(svdVendorSystickConfig))
        {
            svdVendorSystickConfigInt = 1;
        }
        else if("false".equals(svdVendorSystickConfig))
        {
            svdVendorSystickConfigInt = 0;
        }
        else
        {
            svdVendorSystickConfigInt = Integer.valueOf(svdVendorSystickConfig);
        }
        log.trace("Vendor Systick Configuration(int): {}", svdVendorSystickConfigInt);

        Response res = srv.get("architecture", "svd_name=" + svdName);
        if(false == res.wasSuccessfull())
        {
            return false;
        }
        if(0 < res.numResults())
        {
            for(int i = 0; i < res.numResults(); i++)
            {
                String name = res.getString(i, "svd_name");
                if(false == svdName.equals(name))
                {
                    log.trace("Name mismatch: {} - {}", svdName, name);
                    continue;
                }
                // <revision>r1p0</revision>
                String revision = res.getString(i, "revision");
                if(false == svdRevision.equals(revision))
                {
                    log.trace("Revision mismatch: {} - {}", svdRevision, revision);
                    continue;
                }
                // <endian>little</endian>
                String endian = res.getString(i, "endian");
                if(false == svdEndian.equals(endian))
                {
                    log.trace("Endian mismatch: {} - {}", svdEndian, endian);
                    continue;
                }
                // <nvicPrioBits>3</nvicPrioBits>
                int nvicPrioBits = res.getInt(i, "interrupt_prio_bits");
                if(svdNvicPrioBitsInt != nvicPrioBits)
                {
                    log.trace("NVIC bits mismatch: {} - {}", svdNvicPrioBitsInt, nvicPrioBits);
                    continue;
                }
                // <vendorSystickConfig>false</vendorSystickConfig>
                int vendorSystickConfig = res.getInt(i, "ARM_Vendor_systick");
                if(vendorSystickConfig != svdVendorSystickConfigInt)
                {
                    log.trace("Vendor Systick mismatch: {} - {}", svdVendorSystickConfigInt, vendorSystickConfig);
                    continue;
                }
                // we found the right architecture
                int archId = res.getInt(i, "id");
                if(srvArchitectureId == archId)
                {
                    // architecture ID in this microcontroller is already set to the correct value
                    return true;
                }
                else
                {
                    log.trace("Architecture ID mismatch: {} - {}", srvArchitectureId, archId);
                    Response put_res = srv.put("microcontroller", "name=" + device_name + "&architecture_id=" + archId);
                    if(false == put_res.wasSuccessfull())
                    {
                        return false;
                    }
                    else
                    {
                        // we successfully updated the architecture ID for this microcontroller
                        return true;
                    }
                }
            }
            if(0 != srvArchitectureId)
            {
                // this architecture should be on the server, but is not !
                log.error("Could not match architecture from SVD with server architecture id = {}", srvArchitectureId);
                return false;
            }
            // architecture not in database -> create new architecture in database
            log.info("architecture not found -> create new architecture");
            return createArchitectureOnServer(
                    svdName,
                    svdRevision,
                    svdEndian,
                    svdMpuPresentInt,
                    svdFpuPresentInt,
                    svdNvicPrioBitsInt,
                    svdVendorSystickConfigInt );
        }
        else
        {
            // architecture not in database -> create new architecture in database
            log.info("no architectures found -> create new architecture");
            return createArchitectureOnServer(
                    svdName,
                    svdRevision,
                    svdEndian,
                    svdMpuPresentInt,
                    svdFpuPresentInt,
                    svdNvicPrioBitsInt,
                    svdVendorSystickConfigInt );
        }
    }

    private boolean createArchitectureOnServer(
            String svd_name,
            String revision,
            String endian,
            int hasMPU,
            int hasFPU,
            int interrupt_prio_bits,
            int ARM_Vendor_systick )
    {
        StringBuilder sb = new StringBuilder();
        sb.append("name=" + svd_name);
        sb.append("&svd_name=" + svd_name);

        if(null != revision)
        {
            sb.append("&revision=" + revision);
        }

        if(null != endian)
        {
            sb.append("&endian=" + endian);
        }

        sb.append("&hasMPU=" + hasMPU);
        sb.append("&hasFPU=" + hasFPU);
        sb.append("&interrupt_prio_bits=" + interrupt_prio_bits);
        sb.append("&ARM_Vendor_systick=" + ARM_Vendor_systick);

        String param = sb.toString();
        Response res = srv.post("architecture", param);

        if(false == res.wasSuccessfull())
        {
            return false;
        }
        else
        {
            // update microcontroller
            int architectureId = res.getInt("id");
            if(0 != device_id)
            {
                String link_param = "id=" + device_id + "&architecture_id=" + architectureId;
                Response link_res = srv.put("microcontroller", link_param);
                return link_res.wasSuccessfull();
            }
            return true;
        }
    }


    private boolean handleDescription(Element device, Response res)
    {
        Element description = device.getChild("description");
        if(null == description)
        {
            // no description :-(
            // but not required
            return true;
        }
        String svdDescription = description.getText().trim();
        if(1 > svdDescription.length())
        {
            // empty description :-(
            // but not required
            return true;
        }
        String srvDescription = res.getString("description");
        if(true == svdDescription.equals(device_name))
        {
            // uninteresting description :-(
            return true;
        }
        if(false == svdDescription.equals(srvDescription))
        {
            log.info("Description on server : {}, in SVD: {}", srvDescription, svdDescription);
            Response post_res = srv.put("microcontroller", "name=" + device_name + "&description=" + svdDescription);
            if(false == post_res.wasSuccessfull())
            {
                return false;
            }
        }
        // else same description already on server -> OK
        return true;
    }

    private boolean handleAddressUnit(Element device, Response res)
    {
        Element addUnitElement = device.getChild("addressUnitBits");
        if(null == addUnitElement)
        {
            // no Address Unit :-(
            // but not required
            return true;
        }
        int svdAddrUnit = Integer.parseInt(addUnitElement.getText());
        if(0 == svdAddrUnit)
        {
            // empty Address Unit :-(
            // but not required
            return true;
        }
        int srvAddrUnit= res.getInt("Addressable_unit_bit");

        if(srvAddrUnit != svdAddrUnit)
        {
            log.info("Address Unit on server : {}, in SVD: {}", srvAddrUnit, svdAddrUnit);
            Response post_res = srv.put("microcontroller", "name=" + device_name + "&Addressable_unit_bit=" + svdAddrUnit);
            if(false == post_res.wasSuccessfull())
            {
                return false;
            }
        }
        // else same Address Unit already on server -> OK
        return true;
    }

    private boolean handleBitWidth(Element device, Response res)
    {
        Element busWidthElement = device.getChild("width");
        if(null == busWidthElement)
        {
            // no Bus Width :-(
            // but not required
            return true;
        }
        int svdBusWidth = Integer.parseInt(busWidthElement.getText());
        if(0 == svdBusWidth)
        {
            // empty Bus Width :-(
            // but not required
            return true;
        }
        int srvBusWidth= res.getInt("bus_width_bit");

        if(srvBusWidth != svdBusWidth)
        {
            log.info("Bus Width on server : {}, in SVD: {}", srvBusWidth, svdBusWidth);
            Response put_res = srv.put("microcontroller", "name=" + device_name + "&bus_width_bit=" + svdBusWidth);
            if(false == put_res.wasSuccessfull())
            {
                return false;
            }
        }
        // else same Address Unit already on server -> OK
        return true;
    }

    private boolean handlePeripherals(Element device, Response res)
    {
        Element peripherals = device.getChild("peripherals");
        if(null == peripherals)
        {
            // no peripherals in this device ?
            return false;
        }

        int dev_id = device_id;
        // svd_id
        int svd_id = res.getInt("svd_id");
        if(0 != svd_id)
        {
            dev_id = svd_id;
        }

        // prepare peripheral handler
        SvdPeripheralHandler handler = new SvdPeripheralHandler(srv);
        if(false == handler.getAllPeripheralInstancesFromServer(dev_id))
        {
            log.error("Could not read device peripherals from sever");
            return false;
        }

        // default values
        String default_size = device.getChildText("size");
        String default_access = device.getChildText("access");
        String default_resetValue = device.getChildText("resetValue");
        String default_resetMask = device.getChildText("resetMask");
        String default_protection = device.getChildText("protection");
        log.trace("default_size: {}", default_size);
        int default_size_int = 0;
        if(null != default_size)
        {
            if(default_size.startsWith("0x"))
            {
                default_size_int = Integer.valueOf(default_size.substring(2), 16);
            }
            else
            {
                default_size_int = Integer.valueOf(default_size);
            }
        }
        log.trace("default_size(int): {}", default_size_int);
        log.trace("default_access: {}", default_access);
        log.trace("default_resetValue: {}", default_resetValue);
        log.trace("default_resetMask: {}", default_resetMask);
        log.trace("default_protection: {}", default_protection);
        handler.setDefaultSize(default_size_int);
        handler.setDefaultAccess(default_access);
        handler.setDefaultResetValue(default_resetValue);
        handler.setDefaultResetMask(default_resetMask);
        handler.setDefaultProtection(default_protection);

        List<Element> children = peripherals.getChildren();
        Vector<Element> derivedPeripherals = new Vector<Element>();
        HashMap<String, Element> namedPeripherals = new HashMap<String, Element>();
        for(Element peripheral : children)
        {
            String name = peripheral.getChildText("name");
            if(null != name)
            {
                namedPeripherals.put(name, peripheral);
            }
            // check if derived
            // strictly speaking it is probably not necessary to handle the non derived before the derived peripherals.
            // It just feels better and might avoid issues in corner cases, also not much overhead.
            // With a good argument this can be removed.
            String derived = peripheral.getAttributeValue("derivedFrom");
            if(null != derived)
            {
                log.trace("Peripheral: {} is derived from {}", name, derived);
                derivedPeripherals.add(peripheral);
            }
            else
            {
                // not a derived peripheral
                if(false == handler.handle(peripheral))
                {
                    return false;
                }
            }
        }
        log.trace("now handling derived peripherals....");
        for(Element peripheral : derivedPeripherals)
        {
            String derived = peripheral.getAttributeValue("derivedFrom");
            if(false == handler.handleDerived(peripheral, namedPeripherals.get(derived)))
            {
                return false;
            }
        }
        return true;
    }

    public void setVendorName(String vendor_name)
    {
        specified_vendor_name = vendor_name;
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
        if(log.isTraceEnabled())
        {
            List<Attribute> atts = device.getAttributes();
            for(Attribute att : atts)
            {
                log.trace("root: {}: {}", att.getName(), att.getValue());
            }
        }

        // check for unknown children
        List<Element> children = device.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            // all defined child types from SVD standard
            // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_device.html
            case "name":
            case "vendor":
            case "version":
            case "description":
            case "cpu":
            case "addressUnitBits":
            case "width":
            case "peripherals":
            case "vendorID":
            case "series":
            case "licenseText":
            case "headerSystemFilename":
            case "headerDefinitionsPrefix":
            case "size":
            case "access":
            case "resetValue":
            case "resetMask":
            case "protection":
            case "vendorExtensions":
                continue;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown child tag: {}", name);
                return false;
            }
        }

        // extract information
        if(false == handleVendor(device))
        {
            return false;
        }
        if(false == handleName(device))
        {
            return false;
        }
        if(null == device_response)
        {
            return false;
        }
        // check vendor
        int srv_vendor_id = device_response.getInt("vendor_id");
        if(vendor_id != srv_vendor_id)
        {
            log.error("Vendor_id on server: {}, but from SVD: {}!", srv_vendor_id, vendor_id);
            return false;
        }
        if(false == handleDescription(device, device_response))
        {
            return false;
        }
        if(false == handleAddressUnit(device, device_response))
        {
            return false;
        }
        if(false == handleBitWidth(device, device_response))
        {
            return false;
        }
        Element cpuElement = device.getChild("cpu");
        if(false == handleCpuElement(cpuElement, device_response.getInt("architecture_id")))
        {
            return false;
        }

        if(false == handlePeripherals(device, device_response))
        {
            return false;
        }
        // currently ignoring the child elements:
        // --------------------------------------
        // version
        // vendorID
        // series
        // licenseText
        // headerSystemFilename
        // headerDefinitionsPrefix
        // vendorExtensions
        return true;
    }

}
