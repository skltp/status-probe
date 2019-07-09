package se.skltp.components.statusprobe.unit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import se.skltp.components.statusprobe.ProbeOwnStatus;
import se.skltp.components.statusprobe.ProcessingStatusService;
import se.skltp.components.statusprobe.RequestSender;
import se.skltp.components.statusprobe.ServiceResponse;
import se.skltp.components.statusprobe.config.ServicesConfig;
import se.skltp.components.statusprobe.config.StartupException;

import java.io.IOException;
import java.util.HashSet;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
@ContextConfiguration(classes = TestConfig.class)
@WebMvcTest(ProcessingStatusService.class)
public class StatusService_Set_Test {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProbeOwnStatus probeStatus;

    @MockBean
    private ServicesConfig servicesConfig;

    @MockBean
    private RequestSender requestSender;

    private String SERVICE_1 = "Service1";
    private String SERVICE_2 = "Service2";

    private String URL_1 = "http://url1";
    private String URL_2 = "http://url2";



    @Before
    public void before() throws StartupException {
        HashSet<String> hashSet = new HashSet<>();
        hashSet.add(SERVICE_1);
        hashSet.add(SERVICE_2);
        Mockito.when(servicesConfig.getServices()).thenReturn(hashSet);
        Mockito.when(servicesConfig.serviceExists(Mockito.anyString())).thenReturn(true);

        Mockito.when(servicesConfig.getUrl(SERVICE_1)).thenReturn(URL_1);
        Mockito.when(servicesConfig.getUrl(SERVICE_2)).thenReturn(URL_2);

        Mockito.when(servicesConfig.getConnectTimeout(SERVICE_1)).thenReturn(1000);
        Mockito.when(servicesConfig.getConnectTimeout(SERVICE_2)).thenReturn(1000);

        Mockito.when(servicesConfig.getSocketTimeout(SERVICE_1)).thenReturn(1000);
        Mockito.when(servicesConfig.getSocketTimeout(SERVICE_2)).thenReturn(1000);
    }

    @Test
    public void probeStatus_OK_verbose() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(true);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("OK");
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(200, "OK"));

        ResultActions result = mvc.perform(get("/probe").param("verbose", "true"));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.[0].probeMessage").value("OK"));
        result.andExpect(jsonPath("$.[0].probeAvailable").value(true));
        result.andExpect(jsonPath("$.[1].probeMessage").value("OK"));
        result.andExpect(jsonPath("$.[1].probeAvailable").value(true));

        result.andExpect(jsonPath("$.[0].serviceAvailable").value(true));
        result.andExpect(jsonPath("$.[0].serviceMessage").value("OK"));

        result.andExpect(jsonPath("$.[1].serviceAvailable").value(true));
        result.andExpect(jsonPath("$.[1].serviceMessage").value("OK"));
    }

    @Test
    public void probe_Down() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(false);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("DOWN");
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(200, "OK"));

        ResultActions result = mvc.perform(get("/probe"));

        result.andExpect(status().isServiceUnavailable());
        result.andExpect(jsonPath("$", hasSize(2)));
        result.andExpect(jsonPath("$.[0].probeMessage").value("DOWN"));
        result.andExpect(jsonPath("$.[0].probeAvailable").value(false));
        result.andExpect(jsonPath("$.[1].probeMessage").value("DOWN"));
        result.andExpect(jsonPath("$.[1].probeAvailable").value(false));

        result.andExpect(jsonPath("$.[0].serviceAvailable").value(false));
        result.andExpect(jsonPath("$.[0].serviceMessage").isEmpty());

        result.andExpect(jsonPath("$.[1].serviceAvailable").value(false));
        result.andExpect(jsonPath("$.[1].serviceMessage").isEmpty());

    }


    @Test
    public void testService_fail() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(true);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("OK");
        Mockito.when(requestSender.sendStatusRequest(eq(URL_1), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(503, "Fel"));
        Mockito.when(requestSender.sendStatusRequest(eq(URL_2), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(200, "OK"));

        ResultActions result = mvc.perform(get("/probe"));

        result.andExpect(status().isServiceUnavailable());
        result.andExpect(jsonPath("$", hasSize(2)));
        result.andExpect(jsonPath("$.[0].probeMessage").value("OK"));
        result.andExpect(jsonPath("$.[0].probeAvailable").value(true));
        result.andExpect(jsonPath("$.[1].probeMessage").value("OK"));
        result.andExpect(jsonPath("$.[1].probeAvailable").value(true));

        result.andExpect(jsonPath("$.[0].serviceAvailable").value(true));
        result.andExpect(jsonPath("$.[0].serviceMessage").value("OK"));

        result.andExpect(jsonPath("$.[1].serviceAvailable").value(false));
        result.andExpect(jsonPath("$.[1].serviceMessage").value("Fel"));
    }

    @Test
    public void testServiceException() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(true);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("OK");
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenThrow(new IOException("Exception"));

        ResultActions result = mvc.perform(get("/probe"));

        result.andExpect(status().isServiceUnavailable());
        result.andExpect(jsonPath("$", hasSize(2)));
        result.andExpect(jsonPath("$.[0].probeMessage").value("OK"));
        result.andExpect(jsonPath("$.[0].probeAvailable").value(true));
        result.andExpect(jsonPath("$.[1].probeMessage").value("OK"));
        result.andExpect(jsonPath("$.[1].probeAvailable").value(true));

        result.andExpect(jsonPath("$.[0].serviceAvailable").value(false));
        result.andExpect(jsonPath("$.[0].serviceMessage").value("Exception"));

        result.andExpect(jsonPath("$.[1].serviceAvailable").value(false));
        result.andExpect(jsonPath("$.[1].serviceMessage").value("Exception"));

    }


    @Test
    public void testService_ok_verbose() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(true);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("OK");
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(200, "OK"));

        ResultActions result = mvc.perform(get("/probe").param("verbose", "true"));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$", hasSize(2)));
        result.andExpect(jsonPath("$.[0].probeMessage").value("OK"));
        result.andExpect(jsonPath("$.[0].probeAvailable").value(true));
        result.andExpect(jsonPath("$.[1].probeMessage").value("OK"));
        result.andExpect(jsonPath("$.[1].probeAvailable").value(true));

        result.andExpect(jsonPath("$.[0].serviceAvailable").value(true));
        result.andExpect(jsonPath("$.[0].serviceMessage").value("OK"));

        result.andExpect(jsonPath("$.[1].serviceAvailable").value(true));
        result.andExpect(jsonPath("$.[1].serviceMessage").value("OK"));

    }

    @Test
    public void testService_ok() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(true);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("OK");
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(200, "OK"));

        ResultActions result = mvc.perform(get("/probe"));

        result.andExpect(status().isOk());

        String ok_response = result.andReturn().getResponse().getContentAsString();

        assertEquals("OK", ok_response.trim());
    }

}
