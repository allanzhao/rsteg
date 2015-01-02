package com.bitwiseops.rsteg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a stream of bytes to consecutive packets. Groups of three bytes are
 * packed into groups of two 12-bit symbols.
 */
public class RStegPacketOutputStream extends OutputStream {
    private final int packetLength;
    private final int packetLengthBytes;
    private final List<int[]> packets = new ArrayList<int[]>();
    private int byteIndex;
    
    public RStegPacketOutputStream(int packetLength) {
        if(packetLength % 2 != 0) {
            throw new IllegalArgumentException("packetLength must be even.");
        }
        this.packetLength = packetLength;
        this.packetLengthBytes = packetLength / 2 * 3;
        reset();
    }
    
    @Override
    public void write(int b) throws IOException {
        requestCapacity(1);
        storeByte(b);
        byteIndex++;
    }
    
    public List<int[]> getPackets() {
        return packets;
    }
    
    public void reset() {
        packets.clear();
        startNewPacket();
    }
    
    private void storeByte(int b) {
        int[] currentPacket = packets.get(packets.size() - 1);
        switch(byteIndex % 3) {
        case 0:
            currentPacket[byteIndex / 3 * 2] |= b;
            break;
        case 1:
            currentPacket[byteIndex / 3 * 2] |= (b & 0x0f) << 8;
            currentPacket[byteIndex / 3 * 2 + 1] |= (b & 0xf0) >>> 4;
            break;
        case 2:
            currentPacket[byteIndex / 3 * 2 + 1] |= b << 4;
            break;
        }
    }
    
    private int requestCapacity(int byteCount) {
        if(currentPacketCapacityBytes() > 0) {
            return currentPacketCapacityBytes();
        }
        startNewPacket();
        return currentPacketCapacityBytes();
    }
    
    public int currentPacketCapacityBytes() {
        return packetLengthBytes - byteIndex;
    }
    
    private void startNewPacket() {
        packets.add(new int[packetLength]);
        byteIndex = 0;
    }
}
