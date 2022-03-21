package org.chipselect.importer.server;

public abstract class RestServer implements Server
{
    public abstract Response execute(Request req);
    public abstract String getStatus();
}
