package com.bitwiseops.rsteg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class IOUtils {
    private static final int BUFFER_SIZE = 4096;
    
    private IOUtils() {}
    
    public static byte[] readStreamFully(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            int length;
            while((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }
}
