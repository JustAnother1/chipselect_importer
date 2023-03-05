package org.chipselect.importer.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.chipselect.importer.Tool;
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
    private Vector<SeggerDevice> devices;
    private HashMap<String, Integer> architectureIds = new HashMap<String, Integer>();

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

        devices = new Vector<SeggerDevice>();

        List<Element> children = device.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            if(false == "VendorInfo".equals(name))
            {
                log.error("unexpected XML element {} on top level (VendorInfo) !", name);
                return false;
            }
            if(false == handleVendor(child))
            {
                return false;
            }
        }
        // parsed everything!
        log.trace("found {} segger devices!", devices.size());
        int numUseable = 0;
        for(int i = 0; i < devices.size(); i++)
        {
            SeggerDevice dev = devices.get(i);
            if(true == dev.hasRAM())
            {
                String Name = dev.getDeviceName();
                Name = Tool.cleanupString(Name);
                if(true == Name.contains(" ("))
                {
                    // "(allow opt. bytes)" adds memory to a chip that in reality does not have it.
                    // -> ignore that
                    continue;
                    // remove the specifiers found in the SEGGER data like :  "(allow security)", "(allow ECRP)", "(allow opt. bytes)",...
                    // Name = Name.substring(0, Name.indexOf('('));
                    // Name = Name.trim();
                }

                // Statistic
                numUseable++;
                numUseable += dev.getNumberOfAliases();
                // Architecture ID
                int architectureId = 0;
                String architectureName = dev.getCoreName();
                architectureId = getArchitectureIdFor(architectureName);
                if(0 == architectureId)
                {
                    log.error("Architecture {} not on the server !", architectureName);
                    return false;
                }
                dev.setArchitectureId(architectureId);

                if(false == addOrUpdateMicrocontroller(dev, Name))
                {
                    return false;
                }
                int numAlias = dev.getNumberOfAliases();
                if(0 < numAlias)
                {
                    for(int a = 0; a < numAlias; a++)
                    {
                        String Alias = dev.getAlias(a);
                        Alias = Tool.cleanupString(Alias);
                        if(false == addOrUpdateMicrocontroller(dev, Alias))
                        {
                            return false;
                        }
                    }
                }
            }
            // else no new information attached to this device
        }
        log.trace("-> {} useable devices!", numUseable);
        return true;
    }

    private boolean addOrUpdateFlashBanks(SeggerDevice dev, int device_id)
    {
        int numBanks = dev.getNumberOfFlashBanks();
        if(0 < numBanks)
        {
            // get flash banks from server
            Request req = new Request("flash_bank", Request.GET);
            req.addPostParameter("dev_id", device_id);
            Response res = srv.execute(req);
            if(false == res.wasSuccessfull())
            {
                return false;
            }
            HashMap<HexString, Integer> banks = new HashMap<HexString, Integer>();
            HashMap<Integer, HexString> sizes = new HashMap<Integer, HexString>();
            for(int i = 0; i < res.numResults(); i++)
            {
                HexString StartAddress = new HexString(res.getString(i, "start_address"));
                int id = res.getInt(i, "id");
                HexString Size = new HexString(res.getString(i, "size"));
                banks.put(StartAddress, id);
                sizes.put(id, Size);
            }
            for(int i = 0; i < numBanks; i++)
            {
                HexString FlashAddress = new HexString(dev.getFlashStartAddress(i));
                HexString FlashSize = new HexString(dev.getFlashSize(i));
                log.trace("FlashAddress: {}", FlashAddress);
                // check if this bank is already on the server
                if(true == banks.containsKey(FlashAddress))
                {
                    int serverId = banks.get(FlashAddress);
                    HexString ServerSize = sizes.get(serverId);
                    if(false == FlashSize.equals(ServerSize))
                    {
                        log.trace("PUT : size changed from {} to {}", ServerSize, FlashSize);
                        // update size on server
                        Request updateReq = new Request("flash_bank", Request.PUT);
                        updateReq.addPostParameter("id", serverId);
                        updateReq.addPostParameter("size", FlashSize.toString());
                        Response updateRes = srv.execute(updateReq);
                        if(false == updateRes.wasSuccessfull())
                        {
                            return false;
                        }
                        // else OK
                    }
                    // else -> already up to date
                }
                else
                {
                    // if not than add it to the server
                    // update size on server
                    Request addReq = new Request("flash_bank", Request.POST);
                    addReq.addPostParameter("dev_id", device_id);
                    addReq.addPostParameter("start_address", FlashAddress.toString());
                    addReq.addPostParameter("size", FlashSize.toString());
                    Response addRes = srv.execute(addReq);
                    if(false == addRes.wasSuccessfull())
                    {
                        return false;
                    }
                    // else OK
                }
            }
        }
        // else -> another job well done ;-)
        return true;
    }

    private boolean addOrUpdateMicrocontroller(SeggerDevice dev, String Name)
    {
        // ask server
        Request req = new Request("microcontroller", Request.GET);
        req.addPostParameter("name", Name);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not read the microcontroller {} from the server", Name);
            return false;
        }

        int segArchitectureId = dev.getArchitectureId();
        String segRamSize = dev.getRamSize();
        segRamSize = segRamSize.trim();
        String segRamAddr = dev.getRamSize();
        segRamAddr = segRamAddr.trim();
        int segVendorId = dev.getVendorId();
        int device_id = 0;

        if(0 < res.numResults())
        {
            // Server knows this device
            boolean hasChanged = false;
            if(1 != res.numResults())
            {
                log.error("something is wrong on the server");
                return false;
            }

            // Update ?
            // Vendor
            int srvVendorId = res.getInt("vendor_id");
            if(srvVendorId != segVendorId)
            {
                log.debug("Vendor ID changed for device {} seg VendorName : {}", Name, dev.getVendorName());
                log.debug("Vendor ID changed ! SEGGER: {}, Server: {}", segVendorId, srvVendorId);
                if(0 == srvVendorId)
                {
                    srvVendorId = segVendorId;
                    log.trace("PUT: vendor ID changed from 0 to {}", segVendorId);
                    hasChanged = true;
                }
                // else the server has a vendor, and SEGGER probably points to an alternative,...
            }

            // Architecture
            int srvArchitectureId = res.getInt("architecture_id");
            if(srvArchitectureId != segArchitectureId)
            {
                log.debug("Architecture ID changed ! SEGGER: {}, Server: {}", segArchitectureId, srvArchitectureId);
                // The id on the server can be more specific than the rather general term used in the SEGGER file.
                // therefore if the server has a value than that is better than whatever SEGGER has.
                if(0 == srvArchitectureId)
                {
                    srvArchitectureId = segArchitectureId;
                }

            }

            // RAM Size Bytes
            // RAM_size_byte
            String srvRamSize = res.getString("RAM_size_byte");
            if(false == srvRamSize.equals(segRamSize))
            {
                log.trace("PUT: RAM size changed from {} to {}", srvRamSize, segRamSize);
                hasChanged = true;
            }

            // Ram Start
            // RAM_start_address
            String srvRamAddr = res.getString("RAM_start_address");
            if(false == srvRamAddr.equals(segRamAddr))
            {
                log.trace("PUT: RAM Address changed from {} to {}", srvRamAddr, segRamAddr);
                hasChanged = true;
            }

            if(true == hasChanged)
            {
                // update
                device_id = res.getInt("id");
                Request updateRequest = new Request("microcontroller", Request.PUT);
                updateRequest.addPostParameter("id", device_id);
                updateRequest.addPostParameter("architecture_id", srvArchitectureId);
                updateRequest.addPostParameter("vendor_id", srvVendorId);
                updateRequest.addPostParameter("RAM_size_byte", segRamSize);
                updateRequest.addPostParameter("RAM_start_address", segRamAddr);
                Response update_res = srv.execute(updateRequest);
                if(false == update_res.wasSuccessfull())
                {
                    return false;
                }
            }
        }
        else
        {
            // Server does not know this device
            // -> Add it !
            log.debug("The microcontroller {} is not on the server !", Name);
            Request addRequest = new Request("microcontroller", Request.POST);
            addRequest.addPostParameter("name", Name);
            addRequest.addPostParameter("architecture_id", segArchitectureId);
            addRequest.addPostParameter("vendor_id", segVendorId);
            addRequest.addPostParameter("RAM_size_byte", segRamSize);
            addRequest.addPostParameter("RAM_start_address", segRamAddr);
            Response add_res = srv.execute(addRequest);
            if(false == add_res.wasSuccessfull())
            {
                return false;
            }
            device_id = res.getInt("id");
        }
        if(0 == device_id)
        {
            // we just generated the device -> ask server for the id
            Request idRequest = new Request("microcontroller", Request.GET);
            idRequest.addPostParameter("name", Name);
            Response id_res = srv.execute(idRequest);
            if(false == id_res.wasSuccessfull())
            {
                return false;
            }
            device_id = res.getInt("id");
        }

        // now the server knows about that micrcontroller
        if(false == addOrUpdateFlashBanks(dev, device_id))
        {
            return false;
        }
        return true;
    }

    private int getArchitectureIdFor(String architectureName)
    {
        int architectureId = 0;
        architectureName = Tool.cleanupString(architectureName);
        if(true == architectureIds.containsKey(architectureName))
        {
            // use cached value
            architectureId = architectureIds.get(architectureName);
        }
        else
        {
            // ask server
            Request req = new Request("architecture", Request.GET);
            req.addPostParameter("name", architectureName);
            Response res = srv.execute(req);
            if(false == res.wasSuccessfull())
            {
                log.error("could not read the architecture from the server");
                return 0;
            }
            if(0 < res.numResults())
            {
                if(1 != res.numResults())
                {
                    log.error("something is wrong on the server");
                    return 0;
                }
                architectureId = res.getInt("id");
                architectureIds.put(architectureName, architectureId);
            }
            else
            {
                log.error("the Architecture {} is not known to the server", architectureName);
                architectureId = createArchitectureOnServer(architectureName);
                if(0 != architectureId)
                {
                    architectureIds.put(architectureName, architectureId);
                }
            }
        }
        return architectureId;
    }

    private int createArchitectureOnServer(String Name)
    {
        Request postReq = new Request("architecture", Request.POST);
        postReq.addPostParameter("name", Name);
        Response res = srv.execute(postReq);

        if(false == res.wasSuccessfull())
        {
            log.error("could not create the new architecture on the server");
            return 0;
        }
        else
        {
            int architectureId = res.getInt("id");
            return architectureId;
        }
    }

    private int retrieveVendorId(String vendorName)
    {
        Request req = new Request("vendor", Request.GET);
        req.addPostParameter("name", vendorName);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not read the vendor from the server");
            return -2;
        }
        int id = res.getInt("alternative");
        if(id == 0)
        {
            id = res.getInt("id");
        }
        if(0 == id)
        {
            if(true == "Unspecified".equals(vendorName))
            {
                // This is not a vendor,...
                return 0;
            }
            else
            {
                // this vendor is not on the server
                log.debug("The Vendor {} is not known on the server !", vendorName);
                return -1;
                /*
                Request PostReq = new Request("vendor", Request.POST);
                PostReq.addGetParameter("name", vendorName);
                Response post_res = srv.execute(PostReq);
                if(false == post_res.wasSuccessfull())
                {
                    log.error("could not write the vendor to the server");
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
                }
                */
            }
        }
        else
        {
            return id;
        }

    }

    private boolean handleVendor(Element vendorElement)
    {
        String vendorName = vendorElement.getAttributeValue("Name");
        vendorName = Tool.cleanupString(vendorName);
        int vendor_id = retrieveVendorId(vendorName);
        if( 0 > vendor_id)
        {
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
            SeggerDevice dev = new SeggerDevice(vendorName, vendor_id, child);
            if(false == dev.isValid())
            {
                return false;
            }
            else
            {
                devices.add(dev);
            }
        }
        return true;
    }

}
