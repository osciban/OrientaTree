package com.smov.gabriel.orientatree.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.smov.gabriel.orientatree.model.ActivityLOD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Utilities {


    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    // decode image from file
    public static Bitmap decodeFile(File f, int width, int height, Context applicationContext) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            int scale = calculateInSampleSize(o, width, height);

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            Toast.makeText(applicationContext, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
        }
        return null;
    }


    public static boolean mapDownloaded(ActivityLOD activity, Context applicationContext) {
        boolean res = false;
        ContextWrapper cw = new ContextWrapper(applicationContext);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, activity.getId() + ".png");
        if (mypath.exists()) {
            res = true;
        }
        return res;
    }


}
