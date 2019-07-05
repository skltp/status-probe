package se.skltp.components.statusprobe;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;


@RunWith(SpringRunner.class)
public class ProbeOwnStatusTest {

    @Test
    public void testDown() {
        URL url = getClass().getClassLoader().getResource("probFile_down.txt");

        ProbeOwnStatus ownStatus = new ProbeOwnStatus(url.getPath(), "DOWN");
        ownStatus.updateStatus();

        assertFalse(ownStatus.isProbeAvailable());
        assertThat(ownStatus.getProbeMessage(), CoreMatchers.containsString("DOWN"));

    }

    @Test
    public void testOK() {
        URL url = getClass().getClassLoader().getResource("probFile_ok.txt");

        ProbeOwnStatus ownStatus = new ProbeOwnStatus(url.getPath(), "DOWN");
        ownStatus.updateStatus();

        assertTrue(ownStatus.isProbeAvailable());
        assertThat(ownStatus.getProbeMessage(), CoreMatchers.containsString("OK"));

    }

    @Test
    public void testNotExists() {
        ProbeOwnStatus ownStatus = new ProbeOwnStatus("probFile_not_exists.txt", "DOWN");
        ownStatus.updateStatus();

        assertFalse(ownStatus.isProbeAvailable());
        assertThat(ownStatus.getProbeMessage(), CoreMatchers.containsString("does not exist"));

    }

}

