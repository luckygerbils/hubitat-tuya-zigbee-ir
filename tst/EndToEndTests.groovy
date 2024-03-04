import static org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.Before

/**
 * End to end tests of driver actions
 */
class EndToEndTests {
    private def driver

    @Before
    void setUp() {
        driver = new HubitatDriverFacade()
    }

    @Test
    void testLearn() {
        driver.invokeMethod("learn", null)

        assertEquals([ driver.invokeMethod("newLearnMessage", true) ], driver.sentCommands)

        driver.sentCommands.clear()
        driver.invokeMethod("handleStartTransmit", [
            seq: 1,
            length: 15,
            unk1: 0,
            unk2: 0xe004,
            unk3: 0x01,
            cmd:  0x02,
            unk4: 0,
        ])

        assertEquals([ driver.invokeMethod("newStartTransmitAckMessage", [ 1, 15 ]) ], driver.sentCommands)

        driver.sentCommands.clear()
        driver.invokeMethod("handleAck", [
            cmd: 0x1
        ])

        // Request first chunk
        assertEquals([ driver.invokeMethod("newCodeDataRequestMessage", [ 1, 0 ]) ], driver.sentCommands)

        // Chunk 1
        driver.sentCommands.clear()
        driver.invokeMethod("handleCodeDataResponse", [
            zero: 0,
            seq: 1,
            position: 0,
            msgpart: [0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9] as byte[],
            msgpartcrc: 45
        ])

        // Request second chunk
        assertEquals([ driver.invokeMethod("newCodeDataRequestMessage", [ 1, 10 ]) ], driver.sentCommands)

        // Chunk 2
        driver.sentCommands.clear()
        driver.invokeMethod("handleCodeDataResponse", [
            zero: 0,
            seq: 1,
            position: 10,
            msgpart: [0x0, 0x1, 0x2, 0x3, 0x4] as byte[],
            msgpartcrc: 10
        ])

        assertEquals([ driver.invokeMethod("newDoneSendingMessage", [ 1 ]) ], driver.sentCommands)

        // Done Receiving
        driver.sentCommands.clear()
        driver.invokeMethod("handleDoneReceiving", [
            seq: 1,
            zero: 0,
        ])
        
        assertEquals([ driver.invokeMethod("newLearnMessage", [ false ]) ], driver.sentCommands)
        assertEquals(
            [
                [ 
                    name: "lastLearnedCode", 
                    value: "AAECAwQFBgcICQABAgME", 
                    descriptionText: "SomeDeviceId lastLearnedCode is AAECAwQFBgcICQABAgME"
                ]
            ],
            driver.sentEvents
        )
    }

    @Test
    void testSendCode() {
        driver.invokeMethod("sendCode", "BpoRmhFfAjFgAQNfAnYGgA")

        assertEquals([ driver.invokeMethod("newStartTransmitMessage", [ 1, 100 ]) ], driver.sentCommands)

        // Chunk 1
        driver.sentCommands.clear()
        driver.invokeMethod("handleCodeDataRequest", [
            seq: 1,
            position: 0,
            maxlen: 0x38
        ])

        assertEquals([ driver.invokeMethod("newCodeDataResponseMessage", [ 1, 0, [ 0x7B, 0x22, 0x6B, 0x65, 0x79, 0x5F, 0x6E, 0x75, 0x6D, 0x22, 0x3A, 0x31, 0x2C, 0x22, 0x64, 0x65, 0x6C, 0x61, 0x79, 0x22, 0x3A, 0x33, 0x30, 0x30, 0x2C, 0x22, 0x6B, 0x65, 0x79, 0x31, 0x22, 0x3A, 0x7B, 0x22, 0x6E, 0x75, 0x6D, 0x22, 0x3A, 0x31, 0x2C, 0x22, 0x66, 0x72, 0x65, 0x71, 0x22, 0x3A, 0x33, 0x38, 0x30, 0x30, 0x30, 0x2C, 0x22 ] as byte[], 0xAD ]) ], driver.sentCommands)

        // Chunk 2
        driver.sentCommands.clear()
        driver.invokeMethod("handleCodeDataRequest", [
            seq: 1,
            position: 0x38,
            maxlen: 0x38
        ])

        assertEquals([ driver.invokeMethod("newCodeDataResponseMessage", [ 1, 0x38, [ 0x79, 0x70, 0x65, 0x22, 0x3A, 0x31, 0x2C, 0x22, 0x6B, 0x65, 0x79, 0x5F, 0x63, 0x6F, 0x64, 0x65, 0x22, 0x3A, 0x22, 0x42, 0x70, 0x6F, 0x52, 0x6D, 0x68, 0x46, 0x66, 0x41, 0x6A, 0x46, 0x67, 0x41, 0x51, 0x4E, 0x66, 0x41, 0x6E, 0x59, 0x47, 0x67, 0x41, 0x22, 0x7D, 0x7D ] as byte[], 0x8F ]) ], driver.sentCommands)

        // Done Receiving
        driver.sentCommands.clear()
        driver.invokeMethod("handleDoneSending", [
            seq: 1,
            zero: 0,
        ])

        assertEquals([ driver.invokeMethod("newDoneReceivingMessage", [ 1 ]) ], driver.sentCommands)
    }

    @Test
    void testLearnAndSendViaCodeName() {
        driver.invokeMethod("learn", "SomeCommand")
        driver.invokeMethod("handleStartTransmit", [
            seq: 1,
            length: 10,
            unk1: 0,
            unk2: 0xe004,
            unk3: 0x01,
            cmd:  0x02,
            unk4: 0,
        ])
        driver.invokeMethod("handleAck", [
            cmd: 0x1
        ])
        driver.invokeMethod("handleCodeDataResponse", [
            zero: 0,
            seq: 1,
            position: 0,
            msgpart: [0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9] as byte[],
            msgpartcrc: 45
        ])
        driver.invokeMethod("handleDoneReceiving", [
            seq: 1,
            zero: 0,
        ])

        assertEquals(
            [
                [ 
                    name: "lastLearnedCode", 
                    value: "AAECAwQFBgcICQ==", 
                    descriptionText: "SomeDeviceId lastLearnedCode is AAECAwQFBgcICQ=="
                ]
            ],
            driver.sentEvents
        )

        // Send learned code via code name
        driver.sentCommands.clear()
        driver.invokeMethod("sendCode", "SomeCommand")

        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x0 {01 00 5E 00 00 00 00 00 00 00 04 E0 01 02 00 00}"
            ],
            driver.sentCommands,
        )

        // Chunk 1
        driver.sentCommands.clear()
        driver.invokeMethod("handleCodeDataRequest", [
            seq: 1,
            position: 0,
            maxlen: 0x38
        ])

        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x3 {00 01 00 00 00 00 00 37 7B 22 6B 65 79 5F 6E 75 6D 22 3A 31 2C 22 64 65 6C 61 79 22 3A 33 30 30 2C 22 6B 65 79 31 22 3A 7B 22 6E 75 6D 22 3A 31 2C 22 66 72 65 71 22 3A 33 38 30 30 30 2C 22 AD}"
            ],
            driver.sentCommands
        )

        // Done Receiving
        driver.sentCommands.clear()
        driver.invokeMethod("handleDoneSending", [
            seq: 1,
            zero: 0,
        ])

        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x5 {01 00 00 00}"
            ],
            driver.sentCommands
        )
    }

    @Test
    void testLearnAndSendViaButton() {
        driver.invokeMethod("learn", "SomeCommand")
        driver.invokeMethod("handleStartTransmit", [
            seq: 1,
            length: 10,
            unk1: 0,
            unk2: 0xe004,
            unk3: 0x01,
            cmd:  0x02,
            unk4: 0,
        ])
        driver.invokeMethod("handleAck", [
            cmd: 0x1
        ])
        driver.invokeMethod("handleCodeDataResponse", [
            zero: 0,
            seq: 1,
            position: 0,
            msgpart: [0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9] as byte[],
            msgpartcrc: 45
        ])
        driver.invokeMethod("handleDoneReceiving", [
            seq: 1,
            zero: 0,
        ])

        assertEquals(
            [
                [ 
                    name: "lastLearnedCode", 
                    value: "AAECAwQFBgcICQ==", 
                    descriptionText: "SomeDeviceId lastLearnedCode is AAECAwQFBgcICQ=="
                ]
            ],
            driver.sentEvents
        )

        driver.invokeMethod("mapButton", [ new BigDecimal(1), "SomeCommand" ])

        // Send learned code via mapped button push
        driver.sentCommands.clear()
        driver.invokeMethod("push", new BigDecimal(1))

        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x0 {01 00 5E 00 00 00 00 00 00 00 04 E0 01 02 00 00}"
            ],
            driver.sentCommands,
        )

        // Chunk 1
        driver.sentCommands.clear()
        driver.invokeMethod("handleCodeDataRequest", [
            seq: 1,
            position: 0,
            maxlen: 0x38
        ])

        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x3 {00 01 00 00 00 00 00 37 7B 22 6B 65 79 5F 6E 75 6D 22 3A 31 2C 22 64 65 6C 61 79 22 3A 33 30 30 2C 22 6B 65 79 31 22 3A 7B 22 6E 75 6D 22 3A 31 2C 22 66 72 65 71 22 3A 33 38 30 30 30 2C 22 AD}"
            ],
            driver.sentCommands
        )

        // Done Receiving
        driver.sentCommands.clear()
        driver.invokeMethod("handleDoneSending", [
            seq: 1,
            zero: 0,
        ])

        assertEquals(
            [
                "he cmd 0xDEAD 0xBEEF 0xed00 0x5 {01 00 00 00}"
            ],
            driver.sentCommands
        )
    }

}