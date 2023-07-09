package com.example.prototype1;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class addfilter extends Activity {

    private EditText editName, editLink;
    private LinearLayout container;
    private ImageView imageView;
    private DatabaseHelper dbHelper;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addfilter);

        editName = findViewById(R.id.editName);
        editLink = findViewById(R.id.editLink);
        container = findViewById(R.id.container);
        imageView = findViewById(R.id.imageView);

        dbHelper = new DatabaseHelper(this);

        displayData();
    }

    public void uploadImage(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    public void saveToDatabase(View view) {
        String name = editName.getText().toString().trim();
        String link = editLink.getText().toString().trim();
        byte[] imageData = null;

        if (name.isEmpty() || link.isEmpty()) {
            Toast.makeText(this, "Please enter name and link", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageView.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            imageData = stream.toByteArray();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("link", link);
        values.put("image", imageData);

        long newRowId = db.insert("my_table", null, values);

        if (newRowId != -1) {
            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
            displayData();
            editName.setText("");
            editLink.setText("");
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.GONE);

            // Display the uploaded image
            if (imageData != null) {
                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                imageView.setImageBitmap(imageBitmap);
                imageView.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayData() {
        container.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("my_table", null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String link = cursor.getString(cursor.getColumnIndexOrThrow("link"));
            byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);

            TextView nameTextView = new TextView(this);
            nameTextView.setText("Name: " + name);
            itemLayout.addView(nameTextView);

            TextView linkTextView = new TextView(this);
            linkTextView.setText(Html.fromHtml("<a href=\"" + link + "\">" + link + "</a>"));
            linkTextView.setMovementMethod(LinkMovementMethod.getInstance());
            itemLayout.addView(linkTextView);

            if (imageBytes != null) {
                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(imageBitmap);
                itemLayout.addView(imageView);
            }

            container.addView(itemLayout);
        }

        cursor.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Log.e("MainActivity", "Error loading image: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
