/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see <http://www.gnu.org/licenses/>
 *
 */
package org.chipselect.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/** collection of general utility functions.
 *
 * @author Lars P&ouml;tter
 * (<a href=mailto:Lars_Poetter@gmx.de>Lars_Poetter@gmx.de</a>)
 */
public final class Tool
{
    private Tool()
    {
        // Not used !
    }

    public static String getXMLRepresentationFor(Element tag)
    {
        if(null == tag)
        {
            return "";
        }
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        String res = xmlOutput.outputString(tag);
        return res;
    }

    public static Document getXmlDocumentFrom(String xmlData)
    {
        StringReader stringReader = new StringReader(xmlData);
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(stringReader);
            return doc;
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String curentDateTime()
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String fromExceptionToString(final Throwable e)
    {
        if(null == e)
        {
            return "Exception [null]";
        }
        String res = e.getLocalizedMessage() + "\n" ;
        final StringWriter s = new StringWriter();
        final PrintWriter p = new PrintWriter(s);
        e.printStackTrace(p);
        p.flush();
        res = res + s.toString();
        return "Exception [" +res + "]";
    }

    public static String fromByteBufferToHexString(final byte[] buf)
    {
        if(null == buf)
        {
            return "[]";
        }
        else
        {
            return fromByteBufferToHexString(buf, buf.length, 0);
        }
    }

    public static String fromByteBufferToHexString(final int[] buf)
    {
        if(null == buf)
        {
            return "[]";
        }
        else
        {
            return fromByteBufferToHexString(buf, buf.length, 0);
        }
    }

    public static String fromByteBufferToHexString(final byte[] buf, int length)
    {
        return fromByteBufferToHexString(buf, length, 0);
    }

    public static String fromByteBufferToHexString(int[] buf, int length, int offset)
    {
        if(null == buf)
        {
            return "[]";
        }
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < length; i++)
        {
            sb.append(String.format("%02X", (0xff & buf[i + offset])));
            sb.append(" ");
        }
        return "[" + (sb.toString()).trim() + "]";
    }

    public static String fromByteBufferToHexString(final byte[] buf, int length, int offset)
    {
        if(null == buf)
        {
            return "[]";
        }
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < length; i++)
        {
            sb.append(String.format("%02X", buf[i + offset]));
            sb.append(" ");
        }
        return "[" + (sb.toString()).trim() + "]";
    }

    public static String fromByteBufferToUtf8String(final byte[] buf)
    {
        if(null == buf)
        {
            return "[]";
        }
        return fromByteBufferToUtf8String(buf, buf.length, 0);
    }

    public static String fromByteBufferToUtf8String(final byte[] buf, int length, int offset)
    {
        if(null == buf)
        {
            return "[]";
        }
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < length; i++)
        {
            sb.append((char)buf[i + offset]);
        }
        return "[" + (sb.toString()).trim() + "]";
    }

    public static boolean isValidChar(final char c)
    {
        final char[] validChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                                   'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                                   'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                                   'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                                   '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                   ' ',};
        for(int i = 0; i < validChars.length; i++)
        {
            if(c == validChars[i])
            {
                return true;
            }
        }
        return false;
    }

    public static String onlyAllowedChars(final String src)
    {
        final StringBuffer dst = new StringBuffer();
        for(int i = 0; i < src.length(); i++)
        {
            final char cur = src.charAt(i);
            if(true == isValidChar(cur))
            {
                dst.append(cur);
            }
        }
        return dst.toString();
    }

    public static String cleanupString(final String dirty)
    {
        String wet = dirty.trim();
        // unicode
        wet = wet.replaceAll("\\\\u2013", "–");

        // fix
        wet = wet.replaceAll("&lt", "€lt€");
        wet = wet.replaceAll("&gt", "€gt€");
        wet = wet.replaceAll("&apos", "€apos€");
        wet = wet.replaceAll("&quot", "€quot€");
        wet = wet.replaceAll("&lt", "€lt€");
        wet = wet.replaceAll("&amp", "€amp€");
        // protect
        wet = wet.replaceAll("&lt;", "€lt€");
        wet = wet.replaceAll("&gt;", "€gt€");
        wet = wet.replaceAll("&apos;", "€apos€");
        wet = wet.replaceAll("&quot;", "€quot€");
        wet = wet.replaceAll("&lt;", "€lt€");
        wet = wet.replaceAll("\\n", "€n€");
        // clean
        wet = wet.replaceAll("\n", "€n€");
        wet = wet.replaceAll("\t", " ");
        wet = wet.replaceAll("\r", " ");
        wet = wet.replaceAll(";", " "); // <- this causes issues with the &lt;,..
        wet = wet.replaceAll("\"", " ");// <- this causes issues with the \\n
        wet = wet.replaceAll("&", "&amp;");
        wet = wet.replaceAll("<", "&lt;");
        wet = wet.replaceAll(">", "&gt;");
        wet = wet.replaceAll("'", "&apos;");
        wet = wet.replaceAll("\"", "&quot;");
        wet = wet.replaceAll("<", "&lt;");
        // restore
        wet = wet.replaceAll("€lt€", "&lt;");
        wet = wet.replaceAll("€gt€", "&gt;");
        wet = wet.replaceAll("€apos€", "&apos;");
        wet = wet.replaceAll("€quot€", "&quot;");
        wet = wet.replaceAll("€lt€", "&lt;");
        wet = wet.replaceAll("€amp€", "&amp;");
        wet = wet.replaceAll("€n€", "\\\\n");
        while(true == wet.contains("  "))
        {
            wet = wet.replaceAll("  ", " ");
        }
        String clean = wet.trim();
        return clean;
    }

    public static String reportDifferences(String one, String two)
    {
        StringBuffer sb = new StringBuffer();
        if((one == null) || (two == null))
        {
            if(null == one)
            {
                sb.append("first String is NULL !\n");
            }
            if(null == two)
            {
                sb.append("second String is NULL !\n");
            }
        }
        else
        {
            // one and two are not NULL
            if(one.length() != two.length())
            {
                sb.append("Strings have diffeent lengths ! (" + one.length() + ", " + two.length() + " )\n");
            }
            int minLength = one.length();
            if(two.length()< minLength)
            {
                minLength = two.length();
            }
            int numDifferences = 0;
            for(int i = 0; i < minLength; i++)
            {
                char oc = one.charAt(i);
                char tc = two.charAt(i);
                if(oc != tc)
                {
                    sb.append("difference at index " + i + " first string has '" + oc + "' second string has '" + tc + "'\n" );
                    numDifferences++;
                    if(numDifferences > 10)
                    {
                        break;
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String getStacTrace()
    {
        // Thread.dumpStack();
        final StackTraceElement[] trace = (Thread.currentThread()).getStackTrace();
        if(0 == trace.length)
        {
            return "This task has not been started yet !";
        }
        else
        {
            final StringBuffer res = new StringBuffer();
            for(int i = 0; i < trace.length; i++)
            {
                res.append(trace[i].toString());
                res.append("\n");
            }
            return res.toString();
        }
    }

    public static String validatePath(String path)
    {
        if(null == path)
        {
            return "";
        }
        if(1 > path.length())
        {
            return "";
        }
        if(false == path.endsWith(File.separator))
        {
            return path + File.separator;
        }
        return path;
    }

    public static String getCommitID()
    {
        try
        {
            final InputStream s = Tool.class.getResourceAsStream("/git.properties");
            final BufferedReader in = new BufferedReader(new InputStreamReader(s));

            String id = "";

            String line = in.readLine();
            while(null != line)
            {
                if(line.startsWith("git.commit.id.full"))
                {
                    id = line.substring(line.indexOf('=') + 1);
                }
                line = in.readLine();
            }
            in.close();
            s.close();
            return id;
        }
        catch( Exception e )
        {
            return e.toString();
        }
        /*
        try
        {
            final InputStream s = Tool.class.getResourceAsStream("/commit-id");
            final BufferedReader in = new BufferedReader(new InputStreamReader(s));
            final String commitId = in.readLine();
            final String changes = in.readLine();
            in.close();
            s.close();
            if(null != changes)
            {
                if(0 < changes.length())
                {
                    return commitId + "-(" + changes + ")";
                }
                else
                {
                    return commitId;
                }
            }
            else
            {
                return commitId;
            }
        }
        catch( Exception e )
        {
            return e.toString();
        }
        */
    }


    public static long decode(String val)
    {
        if(null == val)
        {
            return 0;
        }
        if(1> val.length())
        {
            return 0;
        }
        if("0x0".equals(val))
        {
            return 0;
        }
        if((    val.contains("a"))
            || (val.contains("A"))
            || (val.contains("b"))
            || (val.contains("B"))
            || (val.contains("c"))
            || (val.contains("C"))
            || (val.contains("d"))
            || (val.contains("D"))
            || (val.contains("e"))
            || (val.contains("E"))
            || (val.contains("f"))
            || (val.contains("F"))
            || (val.contains("x"))
            || (val.contains("X"))
            )
        {
            // is a Hex number
            if((val.startsWith("0x")) || (val.startsWith("0X")))
            {
                return Long.parseLong(val.substring(2), 16);
            }
            else
            {
                return Long.parseLong(val, 16);
            }
        }
        else
        {
            // "018" is not an illegal octal number but a stupid human not knowing about the octal prefix
            // nobody uses octal anyway -> this is decimal!
            return Long.parseLong(val, 10);
        }
    }

}
