package se.skltp.components.statusprobe.config;

import java.util.List;
import java.util.Set;

public interface ServicesConfig {

    Set<String> getServices() throws StartupException;

    String getUrl(String name) throws StartupException ;

    int getConnectTimeout(String name) throws StartupException;

    int getSocketTimeout(String name) throws StartupException ;

    boolean serviceExists(String name) throws StartupException ;

    List<String> getStatusValues(String name) throws StartupException ;
}
