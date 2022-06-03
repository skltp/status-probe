package se.skltp.components.statusprobe;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceResponse {
    private int statusCode;
    private String message;
}
