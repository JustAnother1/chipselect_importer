package org.chipselect.importer.parser;

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
        // TODO
        return false;
    }

}
