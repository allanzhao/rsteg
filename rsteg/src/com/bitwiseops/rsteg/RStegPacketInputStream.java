package com.bitwiseops.rsteg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

/**
 * Reads the contents of consecutive packets as a stream of bytes. Groups of two
 * 12-bit symbols are unpacked into groups of three bytes.
 */
public class RStegPacketInputStream extends InputStream {
    private final Queue<int[]> packets = new ArrayDeque<int[]>();
    private int byteIndex;
    
    @Override
    public int read() throws IOException {
        int available = requestAvailable(1);
        if(available > 0) {
            int value = readByte();
            byteIndex++;
            return value;
        } else {
            return -1;
        }
    }
    
    public void putPacket(int[] packet) {
        packets.add(packet);
    }
    
    public void putPackets(Collection<int[]> packets) {
        this.packets.addAll(packets);
    }
    
    public void reset() {
        packets.clear();
        startNewPacket();
    }
    
    private int readByte() {
        int[] currentPacket = packets.peek();
        if(currentPacket == null) {
            return -1;
        }
        switch(byteIndex % 3) {
        case 0:
            return currentPacket[byteIndex / 3 * 2] & 0xff;
        case 1:
            return ((currentPacket[byteIndex / 3 * 2] >>> 8) & 0x0f) | ((currentPacket[byteIndex / 3 * 2 + 1] << 4) & 0xf0);
        case 2:
            return (currentPacket[byteIndex / 3 * 2 + 1] >>> 4) & 0xff;
        default:
            assert false;
            return -1;
        }
    }
    
    private int requestAvailable(int byteCount) {
        if(currentPacketAvailableBytes() > 0) {
            return currentPacketAvailableBytes();
        } else if(packets.peek() != null) {
            startNewPacket();
            return currentPacketAvailableBytes();
        } else {
            return 0;
        }
    }
    
    public int currentPacketAvailableBytes() {
        int[] currentPacket = packets.peek();
        if(currentPacket != null) {
            return currentPacket.length / 2 * 3 - byteIndex;
        } else {
            return 0;
        }
    }
    
    private void startNewPacket() {
        packets.remove();
        byteIndex = 0;
    }
}
