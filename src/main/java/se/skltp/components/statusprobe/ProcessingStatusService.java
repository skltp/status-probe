package se.skltp.components.statusprobe;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.skltp.components.statusprobe.config.ServicesConfig;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// todo Hantera exceptions på bra sett
// todo validera response

@Slf4j
@RestController
public class ProcessingStatusService {

    @Value("${probe.return.ok.string}")
    private String defaultOkReturnString;

    private HttpClient client;

    @Autowired
    private ProbeOwnStatus probeStatus;

    @Autowired
    private ServicesConfig servicesConfig;

    @Autowired
    private StatusConverter responseConverter;

    @Autowired
    private RequestSender requestSender;


    @GetMapping(value = "/probe")
    void getStatus(HttpServletResponse response, @RequestParam(name = "verbose", required = false) boolean verbose) throws IOException {
        log.debug("getStatus() called on all resources behind probe using verbose={}", verbose);

        probeStatus.updateStatus();

        if (servicesConfig.getServices().isEmpty()) {
            ProcessingStatus statusProbeOwnStatus = fillProbeStatus(new ProcessingStatus());

            if (!probeStatus.isProbeAvailable()) {
                generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(statusProbeOwnStatus));
            } else {
                if (!verbose) {
                    generateResponse(response, HttpServletResponse.SC_OK, defaultOkReturnString);
                } else {
                    generateResponse(response, HttpServletResponse.SC_OK, responseConverter.convert(statusProbeOwnStatus));
                }
            }
        } else {
            List<ProcessingStatus> services = getServicesToProcess();

            if (!probeStatus.isProbeAvailable()) {
                generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(services));
            } else {
                for (ProcessingStatus status : services) {
                    checkServiceStatus(status);
                }

                for (ProcessingStatus status : services) {
                    if (!status.isServiceAvailable()) {
                        generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(services));
                        return;
                    }
                }
                if (!verbose) {
                    generateResponse(response, HttpServletResponse.SC_OK, defaultOkReturnString);
                } else {
                    generateResponse(response, HttpServletResponse.SC_OK, responseConverter.convert(services));
                }
            }
        }
    }


    @GetMapping("/probe/{service}")
    public void getStatusByName(@PathVariable String service, HttpServletResponse response, @RequestParam(name = "verbose", required = false) boolean verbose) throws IOException {
        log.debug("getStatus() called on resource name={} using verbose={}", service, verbose);

        probeStatus.updateStatus();

        ProcessingStatus processingStatus = fillProbeStatus(new ProcessingStatus());
        fillServiceConfiguration(service, processingStatus);

        if (!servicesConfig.serviceExists(service)) {
            log.error("Requested resource with name: {}, was not found in list of resources in property file", service);
            generateResponse(response, HttpServletResponse.SC_NOT_FOUND, responseConverter.convert(processingStatus));
            return;
        }

        if (!probeStatus.isProbeAvailable()) {
            generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(processingStatus));
            return;
        }

        checkServiceStatus(processingStatus);

        if (!processingStatus.isServiceAvailable()) {
            generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(processingStatus));
        } else {
            if (!verbose) {
                generateResponse(response, HttpServletResponse.SC_OK, defaultOkReturnString);
            } else {
                generateResponse(response, HttpServletResponse.SC_OK, responseConverter.convert(processingStatus));
            }
        }
    }

    private void generateResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().println(message);
        response.setContentType("application/json");
    }

    private List<ProcessingStatus> getServicesToProcess() {
        List<ProcessingStatus> processingStatuses = new ArrayList<>();
        for (String service : servicesConfig.getServices()) {
            ProcessingStatus processingStatus = fillProbeStatus(new ProcessingStatus());
            fillServiceConfiguration(service, processingStatus);
            processingStatuses.add(processingStatus);
        }
        return processingStatuses;
    }

    private ProcessingStatus fillProbeStatus(ProcessingStatus processingStatus) {
        processingStatus.setProbeAvailable(probeStatus.isProbeAvailable());
        processingStatus.setProbeMessage(probeStatus.getProbeMessage());
        return processingStatus;
    }

    private void fillServiceConfiguration(String name, ProcessingStatus processingStatus) {
        processingStatus.setName(name);
        processingStatus.setConnecttimeout(Integer.toString(servicesConfig.getConnectTimeout(name)));
        processingStatus.setResponsetimeout(Integer.toString(servicesConfig.getSocketTimeout(name)));
        processingStatus.setUrl((servicesConfig.getUrl(name)));
    }



    private void checkServiceStatus(ProcessingStatus serviceToProcess) {
        try {
            String url = servicesConfig.getUrl(serviceToProcess.getName());
            int connectionTimeout = servicesConfig.getConnectTimeout(serviceToProcess.getName());
            int responseTimeout = servicesConfig.getSocketTimeout(serviceToProcess.getName());

            ServiceResponse response = requestSender.sendStatusRequest(url, connectionTimeout,responseTimeout);

            serviceToProcess.setServiceMessage(response.getMessage());

            boolean validatingStatus = validateResponse(response.getMessage());
            if (response.getStatusCode() == HttpStatus.OK.value() && validatingStatus) {
                serviceToProcess.setServiceAvailable(true);
            } else {
                serviceToProcess.setServiceAvailable(false);
            }
            log.info("Resource: {}, HTTP status code: {}, URL: {}", serviceToProcess.getName(), response.getStatusCode(), url);

        } catch (Exception e) {
            log.error("Exception: " + e.getMessage() + " occured. Resource: " + serviceToProcess.getName() + ", URL " + serviceToProcess.getUrl());

            serviceToProcess.setServiceAvailable(false);
            serviceToProcess.setServiceMessage(e.getMessage());
        }
    }

    private boolean validateResponse(String response) {
        //todo
        return true;
    }

}
