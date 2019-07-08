package se.skltp.components.statusprobe.config;

import java.util.List;
import java.util.Set;

public interface ServicesConfig {

    Set<String> getServices();

    String getUrl(String name);

    int getConnectTimeout(String name);

    int getSocketTimeout(String name);

    boolean serviceExists(String name);

    List<String> getStatusValues(String name);
}
