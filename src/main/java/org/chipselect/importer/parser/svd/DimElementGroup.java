package org.chipselect.importer.parser.svd;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DimElementGroup
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final int dim;
    private final int dim_increment;
    private final Vector<String> indexValues;
    private boolean valid;

    // compare with: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_special.html#dimElementGroup_gr
    public DimElementGroup(int dim, int dim_increment, String dim_index)
    {
        valid = true;
        this.dim = dim;
        if(2>dim)
        {
            log.error("invalid dim value: {}", dim);
            valid = false;
        }
        this.dim_increment = dim_increment;
        if(1>dim_increment)
        {
            log.error("invalid dimIncrement value: {}", dim_increment);
            valid = false;
        }
        indexValues = new Vector<String>();
        // dim_index Null
        if(null == dim_index)
        {
            for(int i = 0; i < dim; i++)
            {
                indexValues.add("" + i);
            }
        }
        // dim_index: "3-6"
        // regular expression: \d+\h*-\h*\d+
        else if(true == Pattern.matches("\\d+\\h*-\\h*\\d+", dim_index))
        {
            String[] parts = dim_index.split("-");
            if(2 != parts.length)
            {
                log.error("Not a 0-5 type of definition: {}", dim_index);
                valid = false;
            }
            else
            {
                int start = Integer.decode(parts[0]);
                int end = Integer.decode(parts[1]);
                for(int i = start; i <= end; i++)
                {
                    indexValues.add("" + i);
                }
                if(indexValues.size() == dim)
                {
                    // OK !
                }
                else
                {
                    log.error("wrong number of values: {} - {}", dim, indexValues.size());
                    valid = false;
                }
            }
        }
        // dim_index: "A, B, C, D, E"
        // regular expression: .+,.+
        else if(true == Pattern.matches(".+,.+", dim_index))
        {
            String[] parts = dim_index.split(",");
            if(dim != parts.length)
            {
                log.error("wrong number of defined values: {} - {}", dim, parts.length);
                valid = false;
            }
            else
            {
                for(int i = 0; i< parts.length; i++)
                {
                    indexValues.add(parts[i]);
                }
            }
        }
        else
        {
            // invalid definition -> same as NULL
            for(int i = 0; i < dim; i++)
            {
                indexValues.add("" + i);
            }
            log.error("invalid definition: {}", dim_index);
            valid = false;
        }
    }

    public int getNumberElements()
    {
        return dim;
    }

    public int getByteOffsetBytes()
    {
        return dim_increment;
    }

    public boolean isValid()
    {
        return valid;
    }

    // regular expression: \[?\h*\%s\h*\]?
    // matches '%s' or '[%s]'
    public String getElementNameFor(String format, int idx)
    {
        if(null == format)
        {
            return null;
        }
        Pattern p = Pattern.compile("\\[?\\h*\\%s\\h*\\]?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(format);
        String index = indexValues.get(idx);
        String res = m.replaceAll(index);
        return res;
    }

}
