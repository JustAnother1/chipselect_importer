package org.chipselect.importer.server;

public interface Server
{
    Response execute(Request req);
    String getStatus();
    void enableDryRunMode();
    void close();
}
