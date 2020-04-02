package com.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class BarcodeGenerator {

    public static Bitmap createBarcode(String data, String type) throws WriterException {
        int size = 250;
        MultiFormatWriter barcodeWriter = new MultiFormatWriter();
        BitMatrix barcodeBitMatrix;
        if (type.equals(Constants.BARCODE_TYPE_QR)) {//TODO:this is for a better presentation of the barcodes but not using right now
            barcodeBitMatrix = barcodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size);
        }else{
            barcodeBitMatrix = barcodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size);
        }
        Bitmap barcodeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        //encodeAsBitmap(barcode_content, BarcodeFormat.QR_CODE, 150, 150);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                barcodeBitmap.setPixel(x, y, barcodeBitMatrix.get(x, y) ?
                        Color.BLACK : Color.WHITE);
            }
        }
        return pad(barcodeBitmap, 60, 1);
    }
    //Move the code to the middle of the paper
    private static Bitmap pad(Bitmap Src, int padding_x, int padding_y) {
        Bitmap outputimage = Bitmap.createBitmap(Src.getWidth() + padding_x,Src.getHeight() + padding_y, Bitmap.Config.ARGB_8888);
        Canvas can = new Canvas(outputimage);
        can.drawARGB(255,255,255,255); //This represents White color
        can.drawBitmap(Src, padding_x, padding_y, null);
        return outputimage;
    }
}
