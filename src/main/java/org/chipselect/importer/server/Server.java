package org.chipselect.importer.server;

public interface Server {

    Response get(String ressource, String urlGet);

    Response post(String ressource, String urlGet);

    Response put(String ressource, String urlGet);

}
