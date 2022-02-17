package com.exampleepam.restaurant.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileUploadUtil {
    private static final int MENU_IMAGE_HEIGHT = 400;
    private static final int MENU_IMAGE_WIDTH = 400;

    public static void saveFile(String uploadDir, String fileName,
                                MultipartFile multipartFile) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        try (InputStream inputStream = multipartFile.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);

            BufferedImage bufferedImageResized =
                    resizeImage(ImageIO.read(inputStream), MENU_IMAGE_WIDTH, MENU_IMAGE_HEIGHT);
            InputStream resizedInputStream = toInputStream(bufferedImageResized);

            Files.copy(resizedInputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new IOException("Could not save image file: " + fileName, ioe);
        }
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }

    public static InputStream toInputStream(BufferedImage image) {
        InputStream is;
        try (
                ByteArrayOutputStream os = new ByteArrayOutputStream();
        ) {
            ImageIO.write(image, "gif", os);
            is = new ByteArrayInputStream(os.toByteArray());
            return is;


        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}