package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

class PluginDescriptorTest {

    @Test
    void pluginDescriptorIsLoadableByBukkit() throws Exception {
        try (InputStream descriptor = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(descriptor, "plugin.yml must be packaged at the JAR root");
            PluginDescriptionFile description = new PluginDescriptionFile(descriptor);

            assertEquals("Cameraman", description.getName());
            assertEquals("xyz.shibomb.cameraman.CameramanPlugin", description.getMain());
            assertEquals("https://github.com/cherdakmd/CHRDK_fork_cameraman", description.getWebsite());
            assertEquals("26.3", description.getVersion());
            assertEquals("26.3", description.getAPIVersion());
        }
    }
}
