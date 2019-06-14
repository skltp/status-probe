package se.skltp.components.statusprobe;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


@Data
@Component
@Slf4j
public class ProbeOwnStatus {
    private boolean probeAvailable;
    private String probeMessage;

    private File probeFile;
    private String downCriteria;


    ProbeOwnStatus(@Value("${probeservice.file}") String probeFilePath, @Value("${probe.down.crireria}") String downCriteria) {
        this.probeFile = new File(probeFilePath);
        this.downCriteria = downCriteria;
    }

    public void updateStatus() {
        if (!probeFile.exists()) {
            probeMessage = "Configured probeFile " + probeFile + " does not exist, StatusProbe signals unavailable when file is missing";
            probeAvailable = false;
            return;
        }

        //Read status from probeFile
        try {
            String probeStatus = Files.readAllLines(probeFile.toPath()).get(0);
            if (probeStatus == null || downCriteria.equals(probeStatus.trim())) {
                probeMessage = "StatusProbe probeFile signals " + probeStatus + ", no check against producers will be performed";
                probeAvailable = false;
            } else {
                probeMessage = "StatusProbe probeFile signals " + probeStatus;
                probeAvailable = true;
            }
        } catch (IOException e) {
            probeMessage = "A problem occurred in the process of reading the file " + probeFile;
            probeAvailable = false;
        }

        log.info("StatusProbe signals serviceAvailable {} and message {}", probeAvailable, probeMessage);
    }
}
