package se.skltp.components.statusprobe.unit;

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import se.skltp.components.statusprobe.ProbeOwnStatus;
import se.skltp.components.statusprobe.ProcessingStatusService;
import se.skltp.components.statusprobe.config.ServicesConfig;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
@ContextConfiguration(classes = TestConfig.class)
@WebMvcTest(ProcessingStatusService.class)
public class StatusService_withEmptyServicesTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProbeOwnStatus probeStatus;

    @MockBean
    private ServicesConfig servicesConfig;

    @Test
    public void probeStatus_DOWN() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(false);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("DOWN");
        Mockito.when(servicesConfig.getServices()).thenReturn(new HashSet());

        ResultActions result = mvc.perform(get("/probe"));

        result.andExpect(status().isServiceUnavailable());
        result.andReturn().getResponse().getContentAsString();
        result.andExpect(MockMvcResultMatchers.jsonPath("$.probeMessage").value("DOWN"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.probeAvailable").value(false));
    }

    @Test
    public void probeStatus_OK_verbose() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(true);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("OK");
        Mockito.when(servicesConfig.getServices()).thenReturn(new HashSet());

        ResultActions result = mvc.perform(get("/probe").param("verbose", "true"));

        result.andExpect(status().isOk());
        result.andExpect(MockMvcResultMatchers.jsonPath("$.probeMessage").value("OK"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.probeAvailable").value(true));
    }

    @Test
    public void probeStatus_OK() throws Exception {
        Mockito.when(probeStatus.isProbeAvailable()).thenReturn(true);
        Mockito.when(probeStatus.getProbeMessage()).thenReturn("OK");
        Mockito.when(servicesConfig.getServices()).thenReturn(new HashSet());

        ResultActions result = mvc.perform(get("/probe"));

        result.andExpect(status().isOk());
        String ok_response = result.andReturn().getResponse().getContentAsString();
        assertEquals("OK", ok_response.trim());
    }




}
