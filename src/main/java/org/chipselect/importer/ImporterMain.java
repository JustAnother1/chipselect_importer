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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.chipselect.importer.parser.SystemViewDescription;
import org.chipselect.importer.server.HttpRestServer;
import org.chipselect.importer.server.RestServer;
import org.chipselect.importer.server.Server;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class ImporterMain
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private boolean import_svd = false;
    private String svd_FileName = null;
    private String vendor_name = null;
    private String restUrl = null;
    private String restUser = null;
    private String restPassword = null;

    public ImporterMain()
    {
        // nothing to do here
    }

    private void startLogging(final String[] args)
    {
        int numOfV = 0;
        for(int i = 0; i < args.length; i++)
        {
            if(true == "-v".equals(args[i]))
            {
                numOfV ++;
            }
        }

        // configure Logging
        switch(numOfV)
        {
        case 0: setLogLevel("warn"); break;
        case 1: setLogLevel("debug");break;
        case 2:
        default:
            setLogLevel("trace");
            System.err.println("Build from " + Tool.getCommitID());
            break;
        }
    }

    private void setLogLevel(String LogLevel)
    {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try
        {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            final String logCfg =
            "<configuration>" +
              "<appender name='STDERR' class='ch.qos.logback.core.ConsoleAppender'>" +
              "<target>System.err</target>" +
                "<encoder>" +
                  "<pattern>%highlight(%-5level) [%logger{36}] %msg%n</pattern>" +
                "</encoder>" +
              "</appender>" +
              "<root level='" + LogLevel + "'>" +
                "<appender-ref ref='STDERR' />" +
              "</root>" +
            "</configuration>";
            ByteArrayInputStream bin;
            bin = new ByteArrayInputStream(logCfg.getBytes(StandardCharsets.UTF_8));
            configurator.doConfigure(bin);
        }
        catch (JoranException je)
        {
          // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    public void printHelp()
    {
        System.out.println("Importer [Parameters]");
        System.out.println("Parameters:");
        System.out.println("-h / --help                : print this message.");
        System.out.println("-v                         : verbose output for even more messages use -v -v");
        System.out.println("-svd <file name>           : import a svd file.");
        System.out.println("-vendor <vendor name>      : set chip venor name. This is necessary if the vendor name is not contained in the imported file.");
        System.out.println("-REST_URL <URL>            : URL of REST server with chip database.");
        System.out.println("-user <name>               : user name for REST server.");
        System.out.println("-password <password>       : password for REST server.");
    }

    public boolean parseCommandLineParameters(String[] args)
    {
        if(null == args)
        {
            return false;
        }
        for(int i = 0; i < args.length; i++)
        {
            if(true == args[i].startsWith("-"))
            {
                if(true == "-h".equals(args[i]))
                {
                    return false;
                }
                else if(true == "-v".equals(args[i]))
                {
                    // already handled -> ignore
                }
                else if(true == "-svd".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    svd_FileName = args[i];
                    import_svd = true;
                }
                else if(true == "-vendor".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    vendor_name = args[i];
                    import_svd = true;
                }
                else if(true == "-REST_URL".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    restUrl = args[i];
                    if(false == restUrl.endsWith("/"))
                    {
                        restUrl = restUrl + "/";
                    }
                }
                else if(true == "-user".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    restUser = args[i];
                }
                else if(true == "-password".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    restPassword = args[i];
                }
                else
                {
                    System.err.println("Invalid Parameter : " + args[i]);
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    public boolean execute()
    {
        Server chipselect = null;
        if(null != restUrl)
        {
            chipselect = new HttpRestServer(restUrl, restUser, restPassword);
        }
        if(null == chipselect)
        {
            log.error("No Server Specified! Nothing to send imported data to!");
            printHelp();
            return false;
        }
        boolean done_something = false;
        // import a svd file?
        if(true == import_svd)
        {
            // SVD Files are XML
            Document jdomDocument = null;
            File xf = new File(svd_FileName);
            if(true == xf.exists())
            {
                SAXBuilder jdomBuilder = new SAXBuilder();
                log.trace("trying to open {}", svd_FileName);
                try
                {
                    jdomDocument = jdomBuilder.build(svd_FileName);
                    SystemViewDescription parser = new SystemViewDescription(chipselect);
                    if(null != vendor_name)
                    {
                        parser.setVendorName(vendor_name);
                    }
                    if(false == parser.parse(jdomDocument))
                    {
                        return false;
                    }
                    done_something = true;
                }
                catch(FileNotFoundException e)
                {
                    log.error("File not found: {}", svd_FileName);
                    jdomDocument = null;
                }
                catch(JDOMException e)
                {
                    log.error(e.getLocalizedMessage());
                    jdomDocument = null;
                }
                catch (IOException e)
                {
                    log.error(e.getLocalizedMessage());
                    jdomDocument = null;
                }
            }
            else
            {
                log.error("the file {} does not exist.", svd_FileName);
                return false;
            }
        }
        // import something else ?
        return done_something;
    }

    public static void main(String[] args)
    {
        ImporterMain m = new ImporterMain();
        m.startLogging(args);
        if(true == m.parseCommandLineParameters(args))
        {
            if(true == m.execute())
            {
                // OK
                System.exit(0);
            }
            else
            {
                // ERROR
                System.err.println("ERROR: Something went wrong!");
                System.exit(1);
            }
        }
        else
        {
            m.printHelp();
            System.exit(1);
        }
    }

}