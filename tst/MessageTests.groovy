import static org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.Before

import groovy.json.JsonSlurper

/**
 * Tests for formatting/parsing functions for individual message types
 */
class MessageTests {
    private def driver

    @Before
    void setUp() {
        driver = new HubitatDriverFacade()
    }

    @Test
    void testSendLearn() {
        driver.invokeMethod("learn", null)

        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xe004 0x0 {7B 22 73 74 75 64 79 22 3A 30 7D}"
            ],
            driver.sentCommands,
        )

        def payload = driver.sentCommands[0].replaceAll(".*\\{([^}]*)\\}", '$1')
        def decodedPayload = new String(payload.split(" ").collect({x -> Integer.parseInt(x, 16) as byte}) as byte[])
        assertEquals([study: 0], new JsonSlurper().parseText(decodedPayload))
    }

    @Test
    void testSendSendCommand_Basic() {
        driver.invokeMethod("sendCode", "BpoRmhFfAjFgAQNfAnYGgAOACwAxIAdAAUAPgAFAEwF2BkAPAV8CwAdACwAxIAEBdgaAA0ABQAsBMQJAF0ADAzECdgZAB4ADA3YGMQJAAUAHwAMHlreaEZoRMQJADwv//5oRmhExAnYGMQI=")

        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x0 {01 00 DE 00 00 00 00 00 00 00 04 E0 01 02 00 00}"
            ],
            driver.sentCommands,
        )
    }

    @Test
    void testSendSendCommand_SeqIncreases() {
        for (int i = 1; i < 1000; i++) {
            driver.sentCommands.clear()
            driver.invokeMethod("sendCode", "SOME_PAYLOAD")
            def seqBytes = String.format("%04X", i).split("")
            assertEquals(
                "he cmd 0xDEAD 0xBEEF 0xed00 0x0 {${seqBytes[2]}${seqBytes[3]} ${seqBytes[0]}${seqBytes[1]} 5A 00 00 00 00 00 00 00 04 E0 01 02 00 00}".toString(),
                driver.sentCommands[0]
            )
        }
    }

    @Test
    void testParseStartTransmit() {
        assertEquals(
            [
                seq: 1,
                length: 5,
                unk1: 0,
                unk2: 0xE004,
                unk3: 0x01,
                cmd: 0x02,
                unk4: 0,
            ],
            driver.invokeMethod("parseStartTransmit", "01 00 05 00 00 00 00 00 00 00 04 E0 01 02 00 00".split(" ") as List)
        )
    }

    @Test
    void testSendStartTransmit() {
        driver.invokeMethod("sendStartTransmit", [1, 5])
        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x0 {01 00 05 00 00 00 00 00 00 00 04 E0 01 02 00 00}"
            ],
            driver.sentCommands,
        )
    }

    @Test
    void testParseStartTransmitAck() {
        assertEquals(
            [
                zero: 0,
                seq: 1,
                length: 5,
                unk1: 0,
                unk2: 0xE004,
                unk3: 0x01,
                cmd: 0x02,
                unk4: 0,
            ],
            driver.invokeMethod("parseStartTransmitAck", "00 01 00 05 00 00 00 00 00 00 00 04 E0 01 02 00 00".split(" ") as List)
        )
    }

    @Test
    void testSendStartTransmitAck() {
        driver.invokeMethod("sendStartTransmitAck", [[
                zero: 0,
                seq: 1,
                length: 5,
                unk1: 0,
                unk2: 0xE004,
                unk3: 0x01,
                cmd: 0x02,
                unk4: 0,
            ]])
        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x1 {00 01 00 05 00 00 00 00 00 00 00 04 E0 01 02 00 00}"
            ],
            driver.sentCommands,
        )
    }

    @Test
    void testParseCodeDataRequest() {
        assertEquals(
            [
                seq: 1,
                position: 5,
                maxlen: 0x38,
            ],
            driver.invokeMethod("parseCodeDataRequest", "01 00 05 00 00 00 38".split(" ") as List)
        )
    }

    @Test
    void testSendCodeDataRequest() {
        driver.invokeMethod("sendCodeDataRequest", [1, 5])
        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x2 {01 00 05 00 00 00 38}"
            ],
            driver.sentCommands,
        )
    }

    @Test
    void testParseCodeDataResponse() {
        def result = driver.invokeMethod("parseCodeDataResponse", "00 01 00 05 00 00 00 0A 00 01 02 03 04 05 06 07 08 09 2D".split(" ") as List);
        result.msgpart = result.msgpart as List
        assertEquals(
            [
                zero: 0,
                seq: 1,
                position: 5,
                msgpart: ([0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9] as byte[]) as List,
                msgpartcrc: 0x2D,
            ],
            result
        )
    }

    @Test
    void testSendCodeDataResponse() {
        driver.invokeMethod("sendCodeDataResponse", [1, 5, [0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9] as byte[], 0x2D])
        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x3 {00 01 00 05 00 00 00 0A 00 01 02 03 04 05 06 07 08 09 2D}"
            ],
            driver.sentCommands,
        )
    }

    @Test
    void testParseDoneSending() {
        assertEquals(
            [
                zero1: 0,
                seq: 1,
                zero2: 0
            ],
            driver.invokeMethod("parseDoneSending", "00 01 00 00 00".split(" ") as List)
        )
    }

    @Test
    void testSendDoneSending() {
        driver.invokeMethod("sendDoneSending", 1)
        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x4 {00 01 00 00 00}"
            ],
            driver.sentCommands,
        )
    }

    @Test
    void testParseDoneReceiving() {
        assertEquals(
            [
                seq: 1,
                zero: 0
            ],
            driver.invokeMethod("parseDoneReceiving", "01 00 00 00".split(" ") as List)
        )
    }

    @Test
    void testSendDoneReceiving() {
        driver.invokeMethod("sendDoneReceiving", 1)
        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x5 {01 00 00 00}"
            ],
            driver.sentCommands,
        )
    }
}