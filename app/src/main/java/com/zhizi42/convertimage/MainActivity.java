package com.zhizi42.convertimage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    String qqNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText editText = findViewById(R.id.editTextNumber);
        Button button = findViewById(R.id.button);
        ImageView imageViewSrc = findViewById(R.id.imageViewSrc);
        ImageView imageViewBlack = findViewById(R.id.imageViewBlack);
        TextView textView = findViewById(R.id.textView);

        imageViewSrc.setOnClickListener(view -> saveImage(view, imageViewSrc));
        imageViewBlack.setOnClickListener(view -> saveImage(view, imageViewBlack));
        textView.setOnClickListener(view -> {
            new AlertDialog.Builder(this, R.style.Theme_ConvertImage)
                    .setMessage(R.string.text_easter_egg_text)
                    .setTitle(R.string.text_easter_egg_title)
                    .setNeutralButton(R.string.text_dialog_button_ok, (dialogInterface, i) -> {})
                    .show();
        });

        button.setOnClickListener(view -> {
            new Thread() {
                @Override
                public void run() {
                    try {
                        qqNumber = editText.getText().toString();
                        URL url = new URL(String.format("https://q1.qlogo.cn/g?b=qq&nk=%s&s=640", qqNumber));
                        URLConnection urlConnection = url.openConnection();
                        InputStream inputStream = urlConnection.getInputStream();
                        Bitmap bitmapSrc = BitmapFactory.decodeStream(inputStream);
                        MainActivity.this.runOnUiThread(() -> imageViewSrc.setImageBitmap(bitmapSrc));
                        Bitmap bitmapBlack = toBlack(bitmapSrc);
                        MainActivity.this.runOnUiThread(() -> imageViewBlack.setImageBitmap(bitmapBlack));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Snackbar.make(view, R.string.error_url, Snackbar.LENGTH_INDEFINITE).show();
                    }
                }
            }.start();
        });
    }

    public Bitmap toBlack(Bitmap bmpOriginal) {
        int height = bmpOriginal.getHeight();
        int width = bmpOriginal.getWidth();

        Bitmap bitmapBlack = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmapBlack);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(f);
        canvas.drawBitmap(bmpOriginal, 0, 0, paint);
        return bitmapBlack;
    }

    public void saveImage(View view, ImageView imageView) {
        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap,
                        String.format("%s.jpg", qqNumber), "");
                Snackbar.make(view, R.string.text_save_succ, Snackbar.LENGTH_LONG).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, qqNumber);
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    Snackbar.make(view, R.string.text_save_succ, Snackbar.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(getCurrentFocus(), R.string.request_permission_succ, Snackbar.LENGTH_LONG).show();
                return;
            }
        }
        Snackbar.make(getCurrentFocus(), R.string.request_permission_failed, Snackbar.LENGTH_INDEFINITE).show();
    }
}