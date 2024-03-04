import java.util.regex.Pattern

class HubitatDriverFacade {
    Script driver
    StringLog log
    Map state
    List sentCommands
    List sentEvents

    HubitatDriverFacade() {
        final GroovyShell shell = new GroovyShell()
        final String driverText = new File('driver.groovy').text
            .replaceAll(Pattern.compile("// BEGIN METADATA.*// END METADATA", Pattern.DOTALL), "")
        driver = shell.parse(driverText)
        driver.run()

        log = new StringLog()
        state = [:]
        sentCommands = []
        sentEvents = []

        def device = new StubDevice(id: "1234", deviceNetworkId: "DEAD", endpointId: "BEEF")

        driver.binding.setVariable("log", log)
        driver.binding.setVariable("logLevel", "DEBUG")
        driver.binding.setVariable("device", device)
        driver.binding.setVariable("zigbee", new StubZigbeeHelper(device: device))
        driver.binding.setVariable("state", state)
        driver.binding.setVariable("sentCommands", sentCommands)
        driver.binding.setVariable("sentEvents", sentEvents)
        driver.binding.setVariable("encodeToString", { byte[] bytes -> Base64.getEncoder().encodeToString(bytes) })
        driver.binding.setVariable("encodeToString", { byte[] bytes -> Base64.getEncoder().encodeToString(bytes) })
    }

    def invokeMethod(final String methodName, args) {
        return driver.invokeMethod(methodName, args)
    }
}

class StubDevice {
    String id;
    String deviceNetworkId;
    String endpointId;

    String toString() {
        return "SomeDeviceId"
    }
}

class StubZigbeeHelper {
    StubDevice device
}

class StringLog {
    StringWriter out = new StringWriter()
    def info(String message) {
        out.write("[info] ${message}\n")
    }

    def debug(String message) {
        out.write("[debug] ${message}\n")
    }
}