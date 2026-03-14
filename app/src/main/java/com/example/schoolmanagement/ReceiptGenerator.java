package com.example.schoolmanagement;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Simple school-style fee receipt image generator.
 * Generates a clean, minimal receipt as a JPEG image.
 */
public class ReceiptGenerator {

    /**
     * Generates a simple school-style fee receipt image and saves it to the
     * gallery/downloads.
     *
     * @return the saved File name or Path, or null on failure
     */
    public static String generateReceipt(Context context,
            String receiptId,
            String date,
            String studentName,
            String studentClass,
            String parentName,
            String parentEmail,
            long totalFees,
            long amountPaid,
            long totalPaidSoFar,
            String paymentType) {

        int width = 595;
        int height = 500;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // ── Paints ──
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(20);
        titlePaint.setFakeBoldText(true);

        Paint subTitlePaint = new Paint();
        subTitlePaint.setColor(Color.BLACK);
        subTitlePaint.setTextSize(14);
        subTitlePaint.setFakeBoldText(true);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12);

        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextSize(12);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(1);

        Paint dottedLinePaint = new Paint();
        dottedLinePaint.setColor(Color.GRAY);
        dottedLinePaint.setStrokeWidth(0.5f);

        int leftMargin = 50;
        int rightMargin = 545;
        int y = 50;

        // ── School Header ──
        canvas.drawText("SmartConnect School", leftMargin, y, titlePaint);
        y += 22;
        canvas.drawText("FEE RECEIPT", leftMargin, y, subTitlePaint);
        y += 8;
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint);
        y += 20;

        // ── Receipt No & Date ──
        canvas.drawText("Receipt No: " + shortenId(receiptId), leftMargin, y, labelPaint);
        canvas.drawText("Date: " + date, 350, y, labelPaint);
        y += 8;
        canvas.drawLine(leftMargin, y, rightMargin, y, dottedLinePaint);
        y += 22;

        // ── Student Details ──
        canvas.drawText("Student Name:", leftMargin, y, labelPaint);
        canvas.drawText(studentName, 200, y, textPaint);
        y += 20;

        canvas.drawText("Class:", leftMargin, y, labelPaint);
        canvas.drawText(studentClass, 200, y, textPaint);
        y += 20;

        canvas.drawText("Parent Name:", leftMargin, y, labelPaint);
        canvas.drawText(parentName, 200, y, textPaint);
        y += 8;
        canvas.drawLine(leftMargin, y, rightMargin, y, dottedLinePaint);
        y += 22;

        // ── Fee Details Table ──
        canvas.drawText("Particulars", leftMargin, y, subTitlePaint);
        canvas.drawText("Amount (₹)", 420, y, subTitlePaint);
        y += 6;
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint);
        y += 20;

        canvas.drawText("Total Fees", leftMargin, y, textPaint);
        canvas.drawText("₹ " + totalFees, 420, y, textPaint);
        y += 20;

        canvas.drawText("Amount Paid (this receipt)", leftMargin, y, textPaint);
        canvas.drawText("₹ " + amountPaid, 420, y, textPaint);
        y += 20;

        canvas.drawText("Payment Type", leftMargin, y, textPaint);
        canvas.drawText(paymentType != null ? paymentType : "-", 420, y, textPaint);
        y += 20;

        canvas.drawText("Total Paid So Far", leftMargin, y, textPaint);
        canvas.drawText("₹ " + totalPaidSoFar, 420, y, textPaint);
        y += 20;

        long remaining = totalFees - totalPaidSoFar;
        if (remaining < 0)
            remaining = 0;
        canvas.drawText("Balance Remaining", leftMargin, y, textPaint);
        canvas.drawText("₹ " + remaining, 420, y, textPaint);
        y += 8;
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint);
        y += 30;

        // ── Footer ──
        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.GRAY);
        footerPaint.setTextSize(10);
        canvas.drawText("This is a computer-generated receipt. No signature required.", leftMargin, y, footerPaint);
        y += 16;
        canvas.drawText("SmartConnect School Management System", leftMargin, y, footerPaint);

        // ── Save as Image ──
        String fileName = "FeeReceipt_" + shortenId(receiptId) + ".jpg";
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/SmartConnect");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri == null)
                    return null;
                fos = resolver.openOutputStream(imageUri);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "SmartConnect");
                if (!dir.exists())
                    dir.mkdirs();
                File imageFile = new File(dir, fileName);
                fos = new FileOutputStream(imageFile);
            }
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
                return fileName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String shortenId(String id) {
        if (id == null || id.length() <= 8)
            return id != null ? id : "";
        return id.substring(id.startsWith("-") ? 1 : 0, Math.min(8, id.length()));
    }
}
