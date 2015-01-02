package com.bitwiseops.rsteg;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.bitwiseops.rsteg.RStegCodec.ErrorCorrectionLevel;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

public class RSteg {
    private static final ErrorCorrectionLevel DEFAULT_ECLEVEL = ErrorCorrectionLevel.MEDIUM;
    
    public static void main(String[] args) {
        ArgumentParser argParser = ArgumentParsers.newArgumentParser("rsteg");
        argParser.description("Hides messages within images. The method used "
                + "is robust against dimensional changes such as cropping and "
                + "padding, as well as modifications within the image.");
        Subparsers subparsers = argParser.addSubparsers().title("subcommands");
        
        Subparser encodeParser = subparsers.addParser("encode");
        encodeParser.help("embed a message into an image");
        encodeParser.description("Combines a cover image with a message and "
                + "writes it to the specified output file. The file extension "
                + "of OUTPUT determines the type of image to be written (png, "
                + "gif, bmp). The message is read from standard input by "
                + "default, but can also be specified through an option.");
        encodeParser.setDefault("subcommand", new EncodeCommand());
        encodeParser.addArgument("cover")
                .metavar("COVER")
                .type(Arguments.fileType().verifyCanRead())
                .help("path to the cover image");
        encodeParser.addArgument("output")
                .metavar("OUTPUT")
                .type(Arguments.fileType().verifyCanCreate())
                .help("path where the output image will be saved");
        encodeParser.addArgument("-m", "--message")
                .help("use this message instead of reading from stdin");
        int maxEcLevel = RStegCodec.ErrorCorrectionLevel.values().length - 1;
        int defaultEcLevel = DEFAULT_ECLEVEL.ordinal();
        encodeParser.addArgument("-l", "--eclevel")
                .type(Integer.class)
                .choices(Arguments.range(0, maxEcLevel))
                .setDefault(defaultEcLevel)
                .help(String.format("error correction level, %d is default", defaultEcLevel));
        
        Subparser decodeParser = subparsers.addParser("decode");
        decodeParser.help("reveal a message hidden in an image");
        decodeParser.description("Decodes the message hidden in an image "
                + "produced by the encode command and writes it to standard "
                + "output.");
        decodeParser.setDefault("subcommand", new DecodeCommand());
        decodeParser.addArgument("image")
                .metavar("IMAGE")
                .help("path to an image containing an encoded message");
        decodeParser.addArgument("-n", "--no-newline")
                .action(Arguments.storeTrue())
                .help("do not output a newline after the message");
        
        Namespace namespace = null;
        try {
            namespace = argParser.parseArgs(args);
        } catch(ArgumentParserException e) {
            argParser.handleError(e);
            System.exit(1);
        }
        
        Subcommand subcommand = namespace.get("subcommand");
        if(subcommand != null) {
            subcommand.execute(namespace);
        }
    }
    
    private static interface Subcommand {
        public void execute(Namespace namespace);
    }
    
    private static class EncodeCommand implements Subcommand {
        @Override
        public void execute(Namespace namespace) {
            File coverImageFile = new File(namespace.getString("cover"));
            File outputImageFile = new File(namespace.getString("output"));
            
            String outputFileType = fileExtension(outputImageFile.getName()).toLowerCase();
            if(!(outputFileType.equals("png") || outputFileType.equals("gif") || outputFileType.equals("bmp"))) {
                System.err.println(String.format("Unsupported output file type \"%s\".", outputFileType));
                System.exit(1);
            }
            
            String message = namespace.getString("message");
            byte[] data = null;
            if(message != null) {
                data = message.getBytes();
            } else {
                try {
                    data = IOUtils.readStreamFully(System.in);
                } catch(IOException e) {
                    System.err.println(e);
                    System.exit(1);
                }
            }
            
            ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.values()[namespace.getInt("eclevel")];
            
            BufferedImage coverImage = null;
            try {
                coverImage = ImageIO.read(coverImageFile);
            } catch(IOException e) {
                System.err.println(e);
                System.exit(1);
            }
            
            int width = coverImage.getWidth();
            int height = coverImage.getHeight();
            
            BufferedImage intermediateImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D intermediateGraphics = intermediateImage.createGraphics();
            intermediateGraphics.drawImage(coverImage, 0, 0, null);
            intermediateGraphics.dispose();
            Bitfield2D bitfield = new Bitfield2D(width, height);
            RStegCodec rStegCodec = new RStegCodec();
            rStegCodec.setTargetBitfield(bitfield);
            rStegCodec.setErrorCorrectionLevel(ecLevel);
            try {
                rStegCodec.encode(data, 0, data.length);
            } catch(CodecException e) {
                System.err.println(e);
                System.exit(1);
            }
            BufferedImageUtils.putBitplane(intermediateImage, 1, 0, bitfield);
            
            BufferedImage outputImage = new BufferedImage(coverImage.getColorModel(), coverImage.copyData(null), coverImage.isAlphaPremultiplied(), null);
            Graphics2D outputGraphics = outputImage.createGraphics();
            outputGraphics.drawImage(intermediateImage, 0, 0, null);
            outputGraphics.dispose();
            
            try {
                ImageIO.write(outputImage, outputFileType, outputImageFile);
            } catch(IOException e) {
                System.err.println(e);
                System.exit(1);
            }
        }
    }
    
    private static class DecodeCommand implements Subcommand {
        @Override
        public void execute(Namespace namespace) {
            File imageFile = new File(namespace.getString("image"));
            
            BufferedImage image = null;
            try {
                image = ImageIO.read(imageFile);
            } catch(IOException e) {
                System.err.println(e);
                System.exit(1);
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            BufferedImage intermediateImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D intermediateGraphics = intermediateImage.createGraphics();
            intermediateGraphics.drawImage(image, 0, 0, null);
            intermediateGraphics.dispose();
            Bitfield2D bitfield = new Bitfield2D(width, height);
            BufferedImageUtils.getBitplane(intermediateImage, 1, 0, bitfield);
            RStegCodec rStegCodec = new RStegCodec();
            rStegCodec.setTargetBitfield(bitfield);
            byte[] data = null;
            try {
                data = rStegCodec.decode();
            } catch(CodecException e) {
                System.err.println(e);
                System.exit(1);
            }
            
            try {
                System.out.write(data);
            } catch(IOException e) {
                System.err.println(e);
                System.exit(1);
            }
            
            if(!namespace.getBoolean("no_newline")) {
                System.out.println();
            }
            
            System.out.flush();
        }
    }
    
    private static String fileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if(lastDotIndex != -1) {
            return fileName.substring(lastDotIndex + 1);
        } else {
            return null;
        }
    }
}
