package org.chipselect.importer.server;

public interface Server {

    Response get(String ressource, String field, String filter);

    Response get(String ressource, String filter);

    Response post(String ressource, String value);

}
