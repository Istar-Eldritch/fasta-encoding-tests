package io.ruben;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


public class FastaTranscoder {

    private static final Map<Character, String> ENCODING_MAP = new HashMap<>();
    private static final Map<String, Character> DECODING_MAP = new HashMap<>();

    static {
        ENCODING_MAP.put('A', "000");
        ENCODING_MAP.put('C', "001");
        ENCODING_MAP.put('G', "010");
        ENCODING_MAP.put('T', "011");
        ENCODING_MAP.put('N', "100");

        DECODING_MAP.put("000", 'A');
        DECODING_MAP.put("001", 'C');
        DECODING_MAP.put("010", 'G');
        DECODING_MAP.put("011", 'T');
        DECODING_MAP.put("100", 'N');
    }

    public static ByteBuffer encodeChunk(String dnaChunk, StringBuilder leftoverBits) {
        // For 3-bit encoding, each base takes 3 bits. So, 8 bits (1 byte) can hold 2 bases and 2 bits of the third base.
        // Therefore, we need approximately (dnaChunk.length() * 3) / 8 bytes. Add 1 for potential partial bytes.
        ByteBuffer buffer = ByteBuffer.allocate((dnaChunk.length() * 3) / 8 + 1);

        StringBuilder currentByteBinary = new StringBuilder(leftoverBits);
        for (char base : dnaChunk.toCharArray()) {
            String encodedBits = ENCODING_MAP.get(Character.toUpperCase(base));
            if (encodedBits == null) {
                throw new IllegalArgumentException("Invalid DNA base found: " + base);
            }
            currentByteBinary.append(encodedBits);

            while (currentByteBinary.length() >= 8) {
                buffer.put((byte) Integer.parseInt(currentByteBinary.substring(0, 8), 2));
                currentByteBinary.delete(0, 8);
            }
        }
        leftoverBits.setLength(0); // Clear the leftover bits
        leftoverBits.append(currentByteBinary); // Store remaining bits for the next chunk
        return buffer;
    }

    public static StringBuilder decodeChunk(ByteBuffer encodedChunk, StringBuilder leftoverBits) {
        StringBuilder dnaChunkBuilder = new StringBuilder();
        StringBuilder currentBits = new StringBuilder(leftoverBits);

        // Read bytes from the buffer and append their binary representation to currentBits
        while (encodedChunk.hasRemaining()) {
            byte b = encodedChunk.get();
            currentBits.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        // Decode 3-bit sequences
        while (currentBits.length() >= 3) {
            String codon = currentBits.substring(0, 3);
            Character base = DECODING_MAP.get(codon);
            if (base == null) {
                throw new IllegalArgumentException("Invalid encoded sequence found: " + codon);
            }
            dnaChunkBuilder.append(base);
            currentBits.delete(0, 3);
        }

        leftoverBits.setLength(0); // Clear the leftover bits
        leftoverBits.append(currentBits); // Store remaining bits for the next chunk
        return dnaChunkBuilder;
    }

    public static void encodeFile(String inputFilePath, String outputFilePath) throws IOException, IllegalArgumentException {
        final int CHUNK_SIZE = 1024 * 1024; // 1MB chunk size

        try (
            BufferedReader reader = new BufferedReader(new java.io.FileReader(inputFilePath));
            FileOutputStream fos = new FileOutputStream(outputFilePath)
        ) {
            char[] buffer = new char[CHUNK_SIZE];
            int bytesRead;
            StringBuilder leftoverBits = new StringBuilder();
            StringBuilder dnaChunkBuilder = new StringBuilder();

            while ((bytesRead = reader.read(buffer)) != -1) {
                dnaChunkBuilder.setLength(0); // Clear for current chunk
                for (int i = 0; i < bytesRead; i++) {
                    char c = buffer[i];
                    if (!Character.isWhitespace(c)) {
                        dnaChunkBuilder.append(c);
                    }
                }
                if (dnaChunkBuilder.length() > 0) {
                    ByteBuffer encodedBuffer = encodeChunk(dnaChunkBuilder.toString(), leftoverBits);
                    encodedBuffer.flip(); // Prepare for reading
                    byte[] encodedBytes = new byte[encodedBuffer.remaining()];
                    encodedBuffer.get(encodedBytes);
                    fos.write(encodedBytes);
                }
            }

            // Handle any remaining bits after the last chunk
            if (leftoverBits.length() > 0) {
                while (leftoverBits.length() < 8) {
                    leftoverBits.append("0"); // Pad with zeros to complete the last byte
                }
                fos.write((byte) Integer.parseInt(leftoverBits.toString(), 2));
            }

            System.out.println("Transcoding successful!");
            System.out.println("Input: " + inputFilePath);
        }
    }

    public static void decodeFile(String inputFilePath, String outputFilePath) throws IOException, IllegalArgumentException {
        final int CHUNK_SIZE = 1024 * 1024; // 1MB chunk size

        try (
            java.io.FileInputStream fis = new java.io.FileInputStream(inputFilePath);
            java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(outputFilePath))
        ) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            StringBuilder leftoverBits = new StringBuilder();

            while ((bytesRead = fis.read(buffer)) != -1) {
                ByteBuffer encodedBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                StringBuilder dnaChunk = decodeChunk(encodedBuffer, leftoverBits);
                writer.write(dnaChunk.toString());
            }

            // Handle any remaining bits after the last chunk
            if (leftoverBits.length() > 0) {
                // If there are leftover bits, it means the last byte was not fully decoded.
                // This implies an incomplete or malformed encoded file. For now, we'll ignore them,
                // but in a more robust system, this might warrant an error or warning.
                 System.err.println("Warning: Incomplete bits at the end of the file. Possible data corruption or partial file.");
            }

            System.out.println("Decoding successful!");
            System.out.println("Input: " + inputFilePath);
            System.out.println("Output: " + outputFilePath);
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java FastaTranscoder <mode> <inputFile> <outputFile>");
            System.out.println("Modes: encode, decode");
            return;
        }

        String mode = args[0];
        String inputFilePath = args[1];
        String outputFilePath = args[2];

        try {
            if (mode.equalsIgnoreCase("encode")) {
                encodeFile(inputFilePath, outputFilePath);
            } else if (mode.equalsIgnoreCase("decode")) {
                decodeFile(inputFilePath, outputFilePath);
            } else {
                System.err.println("Error: Invalid mode. Please use 'encode' or 'decode'.");
            }
        } catch (IOException e) {
            System.err.println("Error during file I/O: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

