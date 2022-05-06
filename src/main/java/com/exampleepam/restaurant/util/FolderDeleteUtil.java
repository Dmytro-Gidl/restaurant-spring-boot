package com.exampleepam.restaurant.util;

import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Util class for deleting data
 */
public final class FolderDeleteUtil {
    public static final String DISH_IMAGES = "dish-images/";

    private FolderDeleteUtil() {
    }

    public static void deleteDishFolder(Long dishId) {
        String deleteDestination = DISH_IMAGES.concat(dishId.toString());

        try {
            FileUtils.deleteDirectory(new File(deleteDestination));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
