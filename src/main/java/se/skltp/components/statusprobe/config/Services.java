package se.skltp.components.statusprobe.config;

import lombok.Data;

@Data
public class Services {
    private String name;
    private String url;
    private Integer connectTimeout;
    private Integer socketTimeout;
}
