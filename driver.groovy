import static java.util.stream.Collectors.joining
import hubitat.device.Protocol
import org.apache.commons.codec.binary.Base64;

metadata {
    definition (name: "Tuya Zigbee IR Remote Control", namespace: "hubitat.anasta.si", author: "Sean Anastasi") {
        capability "Configuration"
        
        command "learn"
        command "sendCommand", ["String"]
       
        attribute "lastLearnedCode", "STRING"
        
        fingerprint profileId: "0104", inClusters: "0000,0004,0005,0003,ED00,E004,0006", outClusters: "0019,000A", manufacturer: "_TZ3290_7v1k4vufotpowp9z", model: "TS1201", deviceJoinName: "Tuya Zigbee IR Remote Control"
    }

    preferences {
       // None for now -- but for Zigbee devices that offer attributes that
       // can be written to set preferences, they are often included here.
       // Later, we will add conventional Hubitat logging preferences here.
    }
}

def cluster = [
    learn:    0xe004,
    transmit: 0xed00
]

def installed() {
   log.info "installed()"
}

def updated() {
   log.debug "updated()"
}

def configure() {
    log.info "configure()"
   // Using a List here even though we are only returning one command because in real
   // drivers, you are likely to have multiple commands you could add to this List:
   //List<String> cmds = []
   //cmds.add "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}"
   //return cmds // or delayBetween(cmds)
    //return [
    //    "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 ${cluster.learn} {${device.zigbeeId}} {}"   
    //]
}

def parse(String description) {
    def descMap = zigbee.parseDescriptionAsMap(description)
     
    switch (descMap.clusterInt) {
    case 0xE004: // IR learn cluster
        log.debug "learning: ${descMap.data}"
        break
    case 0xed00: // IR transmit cluster
        switch (hubitat.helper.HexUtils.hexStringToInt(descMap.command)) {
        case 0x00: return handleCodeLearned(descMap)
        case 0x01: // msg #1, I think this is just an ACK of the recieved initial msg 0
            log.debug "msg 1 (msg 0 ack) recieved. Data: ${descMap.data}"
            break
        case 0x02: return handleRequestForCodeData(descMap)
        case 0x03: return handleCodeDataResponse(descMap)
        case 0x04: return handleDoneSending(descMap)
        case 0x05: return handleDoneReceiving(descMap)
        case 0x0B: // Probably an ACK. Data seems to indicate the code of the message that was sent and we could use that to trigger the next message
            log.debug "ACK (0x0B) (${descMap.command}) ${descMap.data}"
            break
        default:
            log.debug "unknown command: ${descMap.command}"
        }
        break
    default:
        // Probably not needed in most drivers but might be helpful for
        // debugging -- always logging for now:
        log.debug "unknown cluster ${descMap.cluster}"  
        log.debug "descMap = ${descMap}"
        break
    }
}

/**
 * 0x00 Code Learned
 */
def handleCodeLearned(descMap) {
     def data = [
         seq: descMap.data.subList(0, 2),
         length: descMap.data.subList(2, 6),
         unk1: descMap.data.subList(6, 10),
         unk2: descMap.data.subList(10, 12), //clusterId
         unk3: descMap.data.subList(12, 13),
         cmd: descMap.data.subList(13, 14),
         unk4: descMap.data.subList(14, 16),
     ]
    log.debug "msg 0 (code learned) recieved. Data: ${data}"

    state.length = Integer.parseInt(data.length.reverse().join(""), 16)
    state.buffer = []

    // We respond with two different commands
    // It's unclear to me what this first one means
    def command1 = zigbee.command(0xed00, 0x01, "00 ${descMap.data.join(" ")}")
    log.debug "sending \"${command1}\""
    
    // seq: u32, position: u16, maxlen: u8
    def command2 = zigbee.command(0xed00, 0x02, "${data.seq.join(" ")} 00 00 00 00 38")
    log.debug "sending \"${command2}\""

    sendHubCommand(new hubitat.device.HubMultiAction([
        *command1,
        *command2
    ], Protocol.ZIGBEE))   
}

/**
 * 0x02 Code Data Request
 */
def handleRequestForCodeData(descMap) {
    def data = [
        seq: descMap.data.subList(0, 2), // uint16
        position: descMap.data.subList(2, 6), // uint32
        maxlen: descMap.data.subList(6, 7) //uint 8
    ]
    log.debug "msg 2 (request for code data) recieved. Data: ${data}"

    def position = parseLeInteger(data.position)
    def part = state.commandToSendBytes.subList(position, Math.min(position + 55, state.commandToSendBytes.size())) // Apparently 50 bytes at a time
    def crc = checksum(part)

    /*
    {name: 'zero', type: DataType.uint8},
{name: 'seq', type: DataType.uint16},
{name: 'position', type: DataType.uint32},
{name: 'msgpartlen': type: DataType.uint8},
{name: 'msgpart', type: DataType.octetStr},
{name: 'msgpartcrc', type: DataType.uint8},
*/

    def command = zigbee.command(0xed00, 0x03, "00 ${data.seq.join(" ")} ${formatLeBytes(position, 4)} ${formatLeBytes(part.size(), 1)} ${part.join(" ")} ${formatLeBytes(crc, 1)}")
    sendHubCommand(new hubitat.device.HubMultiAction(command, Protocol.ZIGBEE));
    log.debug "send IRCode part (0x03) with cmd: ${command}"
    /*
const seq = msg.data.seq;
const position = msg.data.position;
const irMsg = messagesGet(msg.endpoint, seq);
const part = irMsg.substring(position, position+0x32);
const sum = calcStringCrc(part);
await msg.endpoint.command('zosungIRTransmit', 'zosungSendIRCode03',
{
zero: 0,
seq: seq,
position: position,
msgpart: Buffer.from(part),
msgpartcrc: sum,
},
{disableDefaultResponse: true});
*/   
}

/**
 * 0x03 Code Data
 */
def handleCodeDataResponse(descMap) {
    def data = [
        zero: descMap.data.subList(0, 1), // uint8
        seq: descMap.data.subList(1, 3),  // uint16
        position: descMap.data.subList(3, 7), // uint32
        msgpartlength: descMap.data.subList(7, 8), // uint8
        msgpart: descMap.data.subList(8, descMap.data.size - 1), // octetStr
        msgpartcrc: descMap.data.subList(descMap.data.size - 1, descMap.data.size) // uint8
    ]
    log.debug "msg 3 (code data) recieved. Data: ${data}"

    def position = Integer.parseInt(data.position.reverse().join(""), 16)
    if (position != state.buffer.size) {
        log.warn "Position mismatch! expected: ${state.buffer.size} was: ${position}"   
    }

    def actualCrc = checksum(data.msgpart)
    def expectedCrc = Integer.parseInt(data.msgpartcrc.join(""), 16)
    if (actualCrc != expectedCrc) {
        log.warn "CRC mismatch! expected: ${expectedCrc} was: ${actualCrc}"
        return;
    }

    state.buffer.addAll(data.msgpart)

    if (state.buffer.size < state.length) {
        def nextPosition = [ state.buffer.size & 0xFF000000, state.buffer.size & 0x00FF0000, state.buffer.size & 0x0000FF00, state.buffer.size & 0x000000FF ]
        .collect({i -> hubitat.helper.HexUtils.integerToHexString(i as int, 1) }).reverse().join(" ")
        //hubitat.helper.HexUtils.integerToHexString(state.buffer.size, 4).split("").collate(2).collect{b -> b.join("")}
        log.debug "'${nextPosition}'"

        sendHubCommand(new hubitat.device.HubMultiAction(zigbee.command(0xed00, 0x02, "${data.seq.join(" ")} ${nextPosition} 38"), Protocol.ZIGBEE))
    } else {
        log.debug "Done? ${state.buffer.size} ${state.length}"
        sendHubCommand(new hubitat.device.HubMultiAction(zigbee.command(0xed00, 0x04, "00 ${data.seq.join(" ")} 00 00"), Protocol.ZIGBEE))
    }   
}

/**
 * 0x04 Done Sending
 */
def handleDoneSending(descMap) {
    def data = [
        seq: descMap.data.subList(0, 2),  // uint16
        zero: descMap.data.subList(0, 2) // uint16
    ]
    log.debug "msg 4 (done sending) recieved. Data: ${data}"
    def cmd = zigbee.command(0xed00, 0x05, "${data.seq.join(" ")} ${data.seq.join(" ")}")
    log.debug "sending: ${cmd}"
    sendHubCommand(new hubitat.device.HubMultiAction(command, Protocol.ZIGBEE))   
}

/**
 * 0x05 Done Receiving
 */
def handleDoneReceiving(descMap) {
    def data = [
        seq: descMap.data.subList(0, 2),  // uint16
        zero: descMap.data.subList(0, 2) // uint16
    ]
    log.debug "msg 5 (all code data sent) recieved. Data: ${data}"

    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.command(0xE004, 0x0, stringToHex('{"study":1}')), Protocol.ZIGBEE))
    def code = Base64.encodeBase64String(state.buffer.collect{hexByte -> Integer.parseInt(hexByte, 16).byteValue()}.toArray() as byte[])
    sendEvent(name: "lastLearnedCode", value: code, descriptionText: "${device} lastLearnedCode is ${code}")   
}

def checksum(byteArray) {
    return byteArray.inject(0, {acc, val -> acc + Integer.parseInt(val, 16)}) % 0x100
}

def stringToHex(String str) {
    return str.getBytes().collect({b -> Integer.toString(b, 16)}).join(' ') 
}

def learn() {
    log.info "learn()"
    def command = zigbee.command(0xE004, 0x0, stringToHex('{"study":0}'))
    log.debug "sending \"${command}\""
    return command
}

def nextSeq() {
    return state.nextSeq = ((state.nextSeq ?: 0) + 1) % 0x10000;
}

/**
 * Format an integer as `numBytes` hex digits, separated by spaces, in little endian byte order.
 * formatLeBytes(2, 4) -> "02 00 00 00"
 * formatLeBytes(257, 4) -> "00 01 00 00"
 */
def formatLeBytes(value, numBytes) { 
    def result = [] 
    for (int p = 0; p < numBytes; p++) { 
        def digit1 = value % 16
        value = value.intdiv(16)
        def digit2 = value % 16 
        result.add(Integer.toHexString(digit2) + Integer.toHexString(digit1))
        value = value.intdiv(16)
    }
    return result.join(" ").toUpperCase()
}

def parseLeInteger(bytes) {
    return Integer.parseInt(bytes.reverse().join(""), 16)
}

def sendCommand(String base64Code) {
    log.info "sendCommand(${command})"
    
    def seq = nextSeq()
    /*
    {name: 'seq', type: DataType.uint16},
    {name: 'length', type: DataType.uint32},
    {name: 'unk1', type: DataType.uint32},
    {name: 'unk2', type: DataType.uint16},
    {name: 'unk3', type: DataType.uint8},
    {name: 'cmd', type: DataType.uint8},
    {name: 'unk4', type: DataType.uint16},
    */
    
    def jsonToSend = "{\"key_num\":1,\"delay\":300,\"key1\":{\"num\":1,\"freq\":38000,\"type\":1,\"key_code\":\"${base64Code}\"}}"
    log.debug "JSON to send: ${jsonToSend}"
    state.commandToSendBytes = stringToHex(jsonToSend).split(" ")
    def totalCommandNumBytes = state.commandToSendBytes.length
    
    /*
    {
                    seq: seq,
                    length: irMsg.length,
                    unk1: 0x00000000,
                    unk2: 0xe004,
                    unk3: 0x01,
                    cmd: 0x02,
                    unk4: 0x0000,
                },*/
    def cmd = zigbee.command(0xed00, 0x0, "${formatLeBytes(seq, 2)} ${formatLeBytes(totalCommandNumBytes, 4)} 00 00 00 00 04 e0 01 02 00 00")
    log.debug "sending: ${cmd}"
    return cmd
}
