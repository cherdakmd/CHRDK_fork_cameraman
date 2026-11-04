package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CameraRouteStoreTest {

    @Test
    void exportFileNamesAreDeterministicAndSanitised() {
        assertEquals("route.yml", CameraRouteStore.exportFileName("route"));
        assertEquals("My_Route-2.yml", CameraRouteStore.exportFileName("My_Route-2"));
    }
}
