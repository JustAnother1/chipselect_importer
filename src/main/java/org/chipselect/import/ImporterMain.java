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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImporterMain
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

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

    public boolean parseCommandLineParameters(String[] args)
    {
        CommandLineParser p = new CommandLineParser();
        if(true == p.parse(args))
        {
            cfg = p.getConfiguration();
            return true;
        }
        else
        {
            return false;
        }
    }

    public static void main(String[] args)
    {
        PuzzlerMain m = new PuzzlerMain();
        m.startLogging(args);
        if(true == m.parseCommandLineParameters(args))
        {
            m.execute();
            if(true == m.successful)
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
            System.exit(1);
        }
    }

}
