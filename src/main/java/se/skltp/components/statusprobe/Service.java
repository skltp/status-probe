package se.skltp.components.statusprobe;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Service {
    private String name;
    private String url;
    private Integer connectTimeout;
    private Integer socketTimeout;
    private List<String> statusValues;

    Service() {
        statusValues = new ArrayList<>();
    }
}
