package org.chipselect.importer.parser;

import java.util.List;
import java.util.Vector;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeggerDevice
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final boolean isValid;
    private final String vendorName;
    private final String deviceName;
    private final String coreName;
    private final String ramStart;
    private final String ramSize;
    private final int vendorId;
    private int architectureId = 0;
    private Vector<String> aliases = new Vector<String>();
    private Vector<String> flashAddr = new Vector<String>();
    private Vector<String> flashSize = new Vector<String>();

    public SeggerDevice(String vendorName, int vendorId, Element deviceInfo)
    {
        this.vendorName = vendorName;
        this.vendorId = vendorId;
        deviceName = deviceInfo.getAttributeValue("Name");
        coreName = deviceInfo.getAttributeValue("Core");
        ramStart = deviceInfo.getAttributeValue("WorkRAMStartAddr");
        ramSize = deviceInfo.getAttributeValue("WorkRAMSize");
        // log.trace("Device {} is a {}", deviceName, coreName);
        // log.trace("RAM is @{} of size {}", ramStart, ramSize);

        List<Element> children = deviceInfo.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            case "FlashBankInfo":
                String flashStartAddress = child.getAttributeValue("StartAddr");
                flashAddr.add(flashStartAddress);
                String flash_size = child.getAttributeValue("Size");
                flashSize.add(flash_size);
                break;

            case "AliasInfo":
                String aliasName = child.getAttributeValue("Name");
                aliases.add(aliasName);
                break;

            default:
                log.error("unexpected XML element {} on device level !", name);
                isValid = false;
                return;
            }
        }

        isValid = true;
    }

    public boolean isValid()
    {
        return isValid;
    }

    public String getAlias(int idx)
    {
        return aliases.get(idx);
    }

    public int getNumberOfAliases()
    {
        return aliases.size();
    }

    public String getFlashStartAddress(int idx)
    {
        return flashAddr.get(idx);
    }

    public String getFlashSize(int idx)
    {
        return flashSize.get(idx);
    }

    public int getNumberOfFlashBanks()
    {
        return flashAddr.size();
    }

    public boolean hasRAM()
    {
        if((null == ramStart) || (null == ramSize))
        {
            return false;
        }
        if((1 > ramStart.length()) || (1 > ramSize.length()))
        {
            return false;
        }
        return true;
    }

    public String getVendorName()
    {
        return vendorName;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public String getCoreName()
    {
        return coreName;
    }

    public String getRamStart()
    {
        return ramStart;
    }

    public String getRamSize()
    {
        return ramSize;
    }

    public void setArchitectureId(int architectureId)
    {
        this.architectureId = architectureId;
    }

    public int getArchitectureId()
    {
        return architectureId;
    }

    public int getVendorId()
    {
        return vendorId;
    }

}
