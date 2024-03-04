import static org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.Before

/**
 * Tests for low level utility functions
 */
class UtilsTests {
    private def driver

    @Before
    void setUp() {
        driver = new HubitatDriverFacade()
    }

    @Test
    void testToPayload_ByteArray() {
        def result = driver.invokeMethod("toPayload", [0x0A, 0x0B, 0x10, 0xAB, 0xDE, 0xAD, 0xBE, 0xEF] as byte[])
        assertEquals("0A 0B 10 AB DE AD BE EF", result)
    }

    @Test
    void testToPayload_Struct() {
        def format = [
            [name: "uint8",    type: "uint8" ],
            [name: "uint16",   type: "uint16" ],
            [name: "uint24",   type: "uint24" ],
            [name: "uint32",   type: "uint32" ],
        ];

        assertEquals(
            "01 02 00 03 00 00 04 00 00 00", 
            driver.invokeMethod("toPayload", [ 
                format, 
                [
                    uint8: 0x01,
                    uint16: 0x02,
                    uint24: 0x03,
                    uint32: 0x04
                ]
            ]))

        assertEquals(
            "11 22 22 33 33 33 44 44 44 44", 
            driver.invokeMethod("toPayload", [
                format, 
                [
                    uint8: 0x11,
                    uint16: 0x2222,
                    uint24: 0x333333,
                    uint32: 0x44444444
                ]
            ]))
    }

    @Test
    void testToPayload_Struct_OctetStr() {
        assertEquals(
            "0A 00 01 02 03 04 05 06 07 08 09", 
            driver.invokeMethod("toPayload", [ 
                [
                    [name: "octetStr", type: "octetStr" ],
                ], 
                [
                    octetStr: [0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09] as byte[],
                ]
            ]))

        // Surrounded by some other values
        assertEquals(
            "99 0A 00 01 02 03 04 05 06 07 08 09 AA", 
            driver.invokeMethod("toPayload", [ 
                [
                    [name: "uint8_1",  type: "uint8" ],
                    [name: "octetStr", type: "octetStr" ],
                    [name: "uint8_2",  type: "uint8" ],
                ], 
                [
                    uint8_1: 0x99,
                    octetStr: [0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09] as byte[],
                    uint8_2: 0xaa,
                ]
            ]))
    }

    @Test
    void testToStruct() {
        def format = [
            [name: "uint8",    type: "uint8" ],
            [name: "uint16",   type: "uint16" ],
            [name: "uint24",   type: "uint24" ],
            [name: "uint32",   type: "uint32" ],
        ];

        assertEquals(
            [
                uint8: 0x01,
                uint16: 0x02,
                uint24: 0x03,
                uint32: 0x04
            ], 
            driver.invokeMethod("toStruct", [ 
                format, 
                "01 02 00 03 00 00 04 00 00 00".split(" ") as List
            ]))

        assertEquals(
            [
                uint8: 0x11,
                uint16: 0x2222,
                uint24: 0x333333,
                uint32: 0x44444444
            ], 
            driver.invokeMethod("toStruct", [
                format, 
                "11 22 22 33 33 33 44 44 44 44".split(" ") as List
            ]))
    }

    @Test
    void testToStruct_OctetStr() {
        // assertEquals doesn't work with arrays so we have to test the fields individually

        def result1 = driver.invokeMethod("toStruct", [ 
                [
                    [name: "octetStr", type: "octetStr" ],
                ], 
                "0A 00 01 02 03 04 05 06 07 08 09".split(" ") as List
            ])
        assertEquals(
            ([0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09] as byte[]) as List, 
            result1.octetStr as List)

        // Surrounded by some other values
        def result2 = driver.invokeMethod("toStruct", [ 
                [
                    [name: "uint8_1",  type: "uint8" ],
                    [name: "octetStr", type: "octetStr" ],
                    [name: "uint8_2",  type: "uint8" ],
                ], 
                "99 0A 00 01 02 03 04 05 06 07 08 09 AA".split(" ") as List
            ])

        assertEquals(0x99, result2.uint8_1)
        assertEquals(
            ([0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09] as byte[]) as List,
            result2.octetStr as List,
        )
        assertEquals(0xAA, result2.uint8_2)
    }

    @Test
    void testChecksum() {
        assertEquals(0, driver.invokeMethod("checksum", [] as byte[]))
        assertEquals(0, driver.invokeMethod("checksum", [0] as byte[]))
        assertEquals(1, driver.invokeMethod("checksum", [1] as byte[]))
        assertEquals(45, driver.invokeMethod("checksum", [0, 1, 2, 3, 4, 5, 6, 7, 8, 9] as byte[]))
        assertEquals(1, driver.invokeMethod("checksum", [255, 2] as byte[]))
    }
}