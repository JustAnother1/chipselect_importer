package org.chipselect.importer.parser;

import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemViewDescription
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public SystemViewDescription()
    {
    }

    public boolean parse(Document doc)
    {
        if(null == doc)
        {
            log.error("XML Document is NULL !");
            return false;
        }
        // TODO Auto-generated method stub
        log.error("not implemented");
        return false;
    }



}
