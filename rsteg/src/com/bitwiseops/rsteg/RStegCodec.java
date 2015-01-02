package com.bitwiseops.rsteg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Implements steganographic encoding and decoding for a single bitplane.
 */
public class RStegCodec {
    public static final int VERSION = 0;
    private static final Field DATA_FIELD = new GFPow2(0x1069, 0xffb);// GF(4096), x^12+x^6+x^5+x^3+1
    private static final CRC METADATA_CRC = new CRC(0x07, 8);
    private static final CRC DATA_CRC = new CRC(0x04c11db7, 32);
    static final int TILE_PAYLOAD_SIZE = 12;
    static final int TILE_PAYLOAD_MASK = (1 << TILE_PAYLOAD_SIZE) - 1;
    static final int TILE_WIDTH = 4;
    static final int TILE_SIZE = TILE_WIDTH * TILE_WIDTH;
    static final int PATCH_WIDTH_TILES = 4;
    static final int PATCH_SIZE_TILES = PATCH_WIDTH_TILES * PATCH_WIDTH_TILES;
    static final int PATCH_WIDTH_BITS = PATCH_WIDTH_TILES * TILE_WIDTH;
    private static final int PARITY_RANDOM_SEED = 0;
    private static final int SCRAMBLE_RANDOM_SEED = 1;
    private static final int SHUFFLE_RANDOM_SEED = 2;
    private static final int[] SCRAMBLE_MASKS = new int[PATCH_SIZE_TILES];
    static final int PATCH_INDEX_TILE_X = 2;
    static final int PATCH_INDEX_TILE_Y = 3;
    static final int METADATA_TILE_X = 3;
    static final int METADATA_TILE_Y = 3;
    static final int PATCH_INDEX_TILE_INDEX = posToLocalTileIndex(PATCH_INDEX_TILE_X, PATCH_INDEX_TILE_Y);
    static final int METADATA_TILE_INDEX = posToLocalTileIndex(METADATA_TILE_X, METADATA_TILE_Y);
    static final int DATA_TILES_PER_PATCH = 14;
    static final int CODEWORD_LENGTH = 256;
    static final int ERASED_SYMBOL = -1;
    static final int INVALID_PATCH_INDEX = 4096 - 1;
    static final int DATA_MAX_LENGTH = 10000000;
    
    private ErrorCorrectionLevel ecLevel;
    public final PatchMetadata patchMetadata = new PatchMetadata();
    private Bitfield2D targetBitfield;
    
    public Bitfield2D getTargetBitfield() {
        return targetBitfield;
    }
    
    public void setTargetBitfield(Bitfield2D bitfield) {
        this.targetBitfield = bitfield;
    }
    
    public ErrorCorrectionLevel getErrorCorrectionLevel() {
        return ecLevel;
    }
    
    public void setErrorCorrectionLevel(ErrorCorrectionLevel ecLevel) {
        this.ecLevel = ecLevel;
    }
    
    public void encode(byte[] data, int offset, int length) throws CodecException {
        int packetLength = ecLevel.messageSymbolCount;
        DATA_CRC.reset();
        DATA_CRC.updateWithInt(length);
        int checksum = DATA_CRC.update(data, offset, length);
        List<int[]> packets;
        try(
                RStegPacketOutputStream packetOutputStream = new RStegPacketOutputStream(packetLength);
                DataOutputStream dataOutputStream = new DataOutputStream(packetOutputStream);
        ) {
            dataOutputStream.writeInt(length);
            dataOutputStream.write(data, offset, length);
            dataOutputStream.writeInt(checksum);
            packets = packetOutputStream.getPackets();
        } catch(IOException e) {
            throw new CodecException("Encoding failed.", e);
        }
        
        encodePackets(packets);
    }
    
    private void encodePackets(List<int[]> packets) throws CodecException {
        int messageSymbolCount = ecLevel.messageSymbolCount;
        int checkSymbolCount = CODEWORD_LENGTH - messageSymbolCount;
        ReedSolomon rsCode = new ReedSolomon(DATA_FIELD, messageSymbolCount, checkSymbolCount);
        List<int[]> codewords = new ArrayList<int[]>();
        for(int[] packet : packets) {
            codewords.add(rsCode.encode(packet, 0, packet.length));
        }
        encodeCodewords(codewords);
    }
    
    private void encodeCodewords(List<int[]> codewords) throws CodecException {
        Random shuffleRandom = new Random(SHUFFLE_RANDOM_SEED);
        Random parityRandom = new Random(PARITY_RANDOM_SEED);
        
        PatchMetadata patchMetadata = new PatchMetadata();
        patchMetadata.version = VERSION;
        patchMetadata.ecLevelId = ecLevel.ordinal();
        
        int widthPatches = targetBitfield.getWidth() / PATCH_WIDTH_BITS;
        int heightPatches = targetBitfield.getHeight() / PATCH_WIDTH_BITS;
        int patchCount = widthPatches * heightPatches;
        int dataTileSlots = widthPatches * heightPatches * DATA_TILES_PER_PATCH;
        if(codewords.size() * CODEWORD_LENGTH > dataTileSlots) {
            throw new CodecException("Too much data to fit in this image.");
        }
        
        List<Integer> patchOrdering = new ArrayList<Integer>(patchCount);
        for(int i = 0; i < patchCount; i++) {
            patchOrdering.add(i);
        }
        Collections.shuffle(patchOrdering, shuffleRandom);
        
        int patchIndex = 0;
        int neededPatchCount = MathUtils.ceilDivide(codewords.size(), DATA_TILES_PER_PATCH) * CODEWORD_LENGTH;
        for(int patchIndexInBitfield : patchOrdering) {
            int xPatch = patchIndexInBitfield % widthPatches;
            int yPatch = patchIndexInBitfield / widthPatches;
            int xMinInPatch = xPatch * PATCH_WIDTH_BITS;
            int yMinInPatch = yPatch * PATCH_WIDTH_BITS;
            int codewordStartIndex = patchIndex / CODEWORD_LENGTH * DATA_TILES_PER_PATCH;
            int symbolIndex = patchIndex % CODEWORD_LENGTH;
            patchMetadata.checksum = patchMetadata.calcChecksum(patchIndex);
            
            for(int yTile = 0; yTile < PATCH_WIDTH_TILES; yTile++) {
                for(int xTile = 0; xTile < PATCH_WIDTH_TILES; xTile++) {
                    int tileIndex = yTile * PATCH_WIDTH_TILES + xTile;
                    int x = xMinInPatch + xTile * TILE_WIDTH;
                    int y = yMinInPatch + yTile * TILE_WIDTH;
                    int tilePayload;
                    if(tileIndex == PATCH_INDEX_TILE_INDEX) {
                        if(patchIndex < neededPatchCount) {
                            tilePayload = patchIndex;
                        } else {
                            tilePayload = INVALID_PATCH_INDEX;
                        }
                    } else if(tileIndex == METADATA_TILE_INDEX) {
                        tilePayload = patchMetadata.getMetadataWord();
                    } else {
                        int codewordIndex = codewordStartIndex + tileIndex;
                        if(codewordIndex < codewords.size()) {
                            tilePayload = codewords.get(codewordIndex)[symbolIndex];
                        } else {
                            tilePayload = 0;
                        }
                    }
                    packTile(x, y, xTile, yTile, tilePayload, parityRandom);
                }
            }
            
            patchIndex++;
        }
    }
    
    private void packTile(int x, int y, int xTile, int yTile, int tilePayload, Random parityRandom) {
        int tileBits = tilePayload ^ scrambleMask(xTile, yTile);
        tileBits |= posToLocalTileIndex(xTile, yTile) << TILE_PAYLOAD_SIZE;
        /*
         * The tile must have even parity. If not, randomly flip one of the
         * bits in the alignment word. The error will be uniformly
         * distributed over the alignment possibilities.
         */
        tileBits ^= calcParity(tileBits) << (TILE_PAYLOAD_SIZE + parityRandom.nextInt(4));
        targetBitfield.setBits(x, y, TILE_SIZE, tileBits, TILE_WIDTH);
    }
    
    public byte[] decode() throws CodecException {
        List<int[]> packets = decodePackets();
        byte[] data;
        int checksum;
        try(
                RStegPacketInputStream packetInputStream = new RStegPacketInputStream();
                DataInputStream dataInputStream = new DataInputStream(packetInputStream);
        ) {
            packetInputStream.putPackets(packets);
            int length = dataInputStream.readInt();
            if(length < 0 || length > DATA_MAX_LENGTH) {
                throw new CodecException("Decoding failed.");
            }
            data = new byte[length];
            dataInputStream.readFully(data);
            checksum = dataInputStream.readInt();
        } catch(IOException e) {
            throw new CodecException("Decoding failed.", e);
        }
        DATA_CRC.reset();
        DATA_CRC.updateWithInt(data.length);
        if(DATA_CRC.update(data, 0, data.length) != checksum) {
            throw new CodecException("Decoding failed: incorrect data checksum.");
        }
        return data;
    }
    
    private List<int[]> decodePackets() throws CodecException {
        Map<Integer, int[]> codewordMap = decodeCodewords();
        int messageSymbolCount = ecLevel.messageSymbolCount;
        int checkSymbolCount = CODEWORD_LENGTH - messageSymbolCount;
        ReedSolomon rsCode = new ReedSolomon(DATA_FIELD, messageSymbolCount, checkSymbolCount);
        List<int[]> packets = new ArrayList<int[]>();
        for(int i = 0; codewordMap.containsKey(i); i++) {
            int[] codeword = codewordMap.get(i);
            /*
             * Current decoder does not support known erasures, so replace
             * ERASED_SYMBOL with an arbitrary value.
             */
            for(int j = 0; j < codeword.length; j++) {
                if(codeword[j] == ERASED_SYMBOL) {
                    codeword[j] = 0;
                }
            }
            packets.add(rsCode.decode(codeword));
        }
        return packets;
    }

    private Map<Integer, int[]> decodeCodewords() throws CodecException {
        int alignment = guessAlignment();
        int xOffset = alignment & 0b1111;
        int yOffset = (alignment >>> 4) & 0b1111;
        PatchMetadata patchMetadata = new PatchMetadata();
        Map<Integer, int[]> codewordMap = new HashMap<Integer, int[]>();
        ModeFinder<Integer> metadataModeFinder = new ModeFinder<Integer>();
        
        for(int yMinInPatch = (yOffset - 16) % 16; yMinInPatch < targetBitfield.getHeight(); yMinInPatch += 16) {
            for(int xMinInPatch = (xOffset - 16) % 16; xMinInPatch < targetBitfield.getWidth(); xMinInPatch += 16) {
                int patchIndex = unpackTileIfExists(xMinInPatch + 8, yMinInPatch + 12, 2, 3);
                int metadataWord = unpackTileIfExists(xMinInPatch + 12, yMinInPatch + 12, 3, 3);
                int symbolIndex = patchIndex % CODEWORD_LENGTH;
                if(patchIndex != ERASED_SYMBOL && patchIndex != INVALID_PATCH_INDEX && metadataWord != ERASED_SYMBOL) {
                    patchMetadata.setMetadataWord(metadataWord);
                    if(patchMetadata.checksumValid(patchIndex)) {
                        metadataModeFinder.add(metadataWord & 0b1111);
                        for(int yTile = 0; yTile < 4; yTile++) {
                            for(int xTile = 0; xTile < 4; xTile++) {
                                int tileIndex = yTile * PATCH_WIDTH_TILES + xTile;
                                if(tileIndex < DATA_TILES_PER_PATCH) {
                                    int symbol = unpackTileIfExists(xMinInPatch + xTile * TILE_WIDTH, yMinInPatch + yTile * TILE_WIDTH, xTile, yTile);
                                    if(symbol != ERASED_SYMBOL) {
                                        int codewordIndex = patchIndex / CODEWORD_LENGTH * DATA_TILES_PER_PATCH + tileIndex;
                                        
                                        int[] codeword;
                                        if(codewordMap.containsKey(codewordIndex)) {
                                            codeword = codewordMap.get(codewordIndex);
                                        } else {
                                            codeword = new int[CODEWORD_LENGTH];
                                            Arrays.fill(codeword, ERASED_SYMBOL);
                                            codewordMap.put(codewordIndex, codeword);
                                        }
                                        
                                        /* TODO: If different patches supply
                                         * different symbols at the same location,
                                         * treat that location as an erasure
                                         */
                                        codeword[symbolIndex] = symbol;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if(metadataModeFinder.hasMode()) {
            patchMetadata.setMetadataWord(metadataModeFinder.getMode());
            if(patchMetadata.version != VERSION) {
                throw new CodecException("Decoding failed: version mismatch.");
            }
            ecLevel = ErrorCorrectionLevel.values()[patchMetadata.ecLevelId];
        }
        
        return codewordMap;
    }
    
    private int unpackTile(int x, int y, int xTile, int yTile) {
        int tileBits = targetBitfield.getBits(x, y, TILE_SIZE, TILE_WIDTH);
        if(calcParity(tileBits) == 0) {
            return (tileBits ^ scrambleMask(xTile, yTile)) & TILE_PAYLOAD_MASK;
        } else {
            return ERASED_SYMBOL;
        }
    }
    
    private int unpackTileIfExists(int x, int y, int xTile, int yTile) {
        if(tileExists(x, y)) {
            return unpackTile(x, y, xTile, yTile);
        } else {
            return ERASED_SYMBOL;
        }
    }
    
    /**
     * Returns the most likely alignment of the patches inside <code>bitfield
     * </code> in packed format.
     */
    private int guessAlignment() {
        int tileAlignment = indexOfMax(calcTileAlignmentConfidences());
        int xOffset = tileAlignment & 0b11;
        int yOffset = (tileAlignment >>> 2) & 0b11;
        
        int patchAlignment = indexOfMax(calcPatchAlignmentConfidences(xOffset, yOffset));
        xOffset += (patchAlignment & 0b11) << 2;
        yOffset += ((patchAlignment >>> 2) & 0b11) << 2;
        
        return (yOffset << 4) | xOffset;
    }
    
    /**
     * Returns an array of 16 values representing the confidences for each
     * possible alignment of tiles within <code>bitfield</code>, based on the
     * proportion of correct parities.
     */
    private float[] calcTileAlignmentConfidences() {
        float[] confidences = new float[TILE_SIZE];
        
        for(int yOffset = 0; yOffset < TILE_WIDTH; yOffset++) {
            for(int xOffset = 0; xOffset < TILE_WIDTH; xOffset++) {
                int count = 0;
                int total = 0;
                for(int y = yOffset; y <= targetBitfield.getHeight() - TILE_WIDTH; y += TILE_WIDTH) {
                    for(int x = xOffset; x <= targetBitfield.getWidth() - TILE_WIDTH; x += TILE_WIDTH) {
                        int bits = targetBitfield.getBits(x, y, TILE_SIZE, TILE_WIDTH);
                        if(calcParity(bits) == 0) {
                            count++;
                        }
                        total++;
                    }
                }
                confidences[yOffset * TILE_WIDTH + xOffset] = (float)count / total;
            }
        }
        
        return confidences;
    }
    
    /**
     * Returns an array of 16 values representing the confidences for each
     * possible alignment of patches within <code>bitfield</code>, assuming
     * that the first whole tile lies at coordinates (<code>xOffset</code>,
     * <code>yOffset</code>).
     */
    private float[] calcPatchAlignmentConfidences(int xOffset, int yOffset) {
        int[] counts = new int[PATCH_SIZE_TILES];
        int total = 0;
        
        int ym = 0;
        for(int y = yOffset; y <= targetBitfield.getHeight() - TILE_WIDTH; y += TILE_WIDTH) {
            int xm = 0;
            for(int x = xOffset; x <= targetBitfield.getWidth() - TILE_WIDTH; x += TILE_WIDTH) {
                int alignmentCode = targetBitfield.getBits(x, y + 3, 4);
                int xAlign = alignmentCode & 0b11;
                int yAlign = (alignmentCode >>> 2) & 0b11;
                counts[(((ym - yAlign) & 0b11) << 2) | ((xm - xAlign) & 0b11)]++;
                total++;
                xm = (xm + 1) & 0b11;
            }
            ym = (ym + 1) & 0b11;
        }
        
        float[] confidences = new float[counts.length];
        for(int i = 0; i < counts.length; i++) {
            confidences[i] = (float)counts[i] / total;
        }
        return confidences;
    }
    
    private boolean tileExists(int x, int y) {
        return (x >= 0) && (x <= targetBitfield.getWidth() - TILE_WIDTH) && (y >= 0) && (y <= targetBitfield.getHeight() - TILE_WIDTH);
    }
    
    /**
     * Returns the packed representation of a tile coordinate within a patch.
     */
    private static int posToLocalTileIndex(int xTile, int yTile) {
        return ((yTile & 0b11) << 2) | (xTile & 0b11);
    }
    
    /**
     * Returns the scramble mask for a tile coordinate (<code>xTile</code>,
     * <code>yTile</code>).
     */
    private static int scrambleMask(int xTile, int yTile) {
        return SCRAMBLE_MASKS[posToLocalTileIndex(xTile, yTile)];
    }
    
    /**
     * Calculates the parity of an integer <code>x</code>, returning 1 for odd
     * parity and 0 for even parity.
     */
    private static int calcParity(int x) {
        x ^= x >>> 16;
        x ^= x >>> 8;
        x ^= x >>> 4;
        x ^= x >>> 2;
        x ^= x >>> 1;
        return x & 1;
    }
    
    private static int indexOfMax(float[] values) {
        float maxValue = Float.MIN_VALUE;
        int indexOfMax = 0;
        for(int i = 0; i < values.length; i++) {
            if(values[i] > maxValue) {
                maxValue = values[i];
                indexOfMax = i;
            }
        }
        return indexOfMax;
    }
    
    static {
        Random random = new Random(SCRAMBLE_RANDOM_SEED);
        for(int i = 0; i < SCRAMBLE_MASKS.length; i++) {
            SCRAMBLE_MASKS[i] = random.nextInt() & 0xfff;
        }
    }
    
    public static enum ErrorCorrectionLevel {
        LOW(0.875f),
        MEDIUM(0.75f),
        HIGH(0.5f),
        VERY_HIGH(0.25f);
        
        private final float dataRate;
        private final int messageSymbolCount;
        
        private ErrorCorrectionLevel(float dataRate) {
            this.dataRate = dataRate;
            this.messageSymbolCount = (int)(CODEWORD_LENGTH * dataRate);
        }
    }
    
    private static class PatchMetadata {
        int version;// 2 bits, meta tile
        int ecLevelId;// 2 bits, meta tile
        int checksum;// 8 bits, meta tile
        
        public int getMetadataWord() {
            int metadataWord = version;
            metadataWord |= ecLevelId << 2;
            metadataWord |= checksum << 4;
            return metadataWord;
        }
        
        public void setMetadataWord(int metadataWord) {
            version = metadataWord & 0b11;
            ecLevelId = (metadataWord >>> 2) & 0b11;
            checksum = (metadataWord >>> 4) & 0b11111111;
        }
        
        public boolean checksumValid(int patchIndex) {
            return checksum == calcChecksum(patchIndex);
        }
        
        public int calcChecksum(int patchIndex) {
            METADATA_CRC.reset();
            METADATA_CRC.update((byte)version);
            METADATA_CRC.update((byte)ecLevelId);
            return METADATA_CRC.updateWithInt(patchIndex);
        }
    }
}
