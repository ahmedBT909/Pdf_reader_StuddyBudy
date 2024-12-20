package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnPdfSelectListener {

    private MainAdabter mainAdabter;
    private List<File> pdfList;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        runTimePermission();

        Button btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnSelectPdf.setOnClickListener(v -> openFileChooser());
    }

    private void runTimePermission() {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        displayPdf();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Log.e("Permission", "Permission Denied");
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, com.karumi.dexter.PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public ArrayList<File> findPdf(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();

        if (files == null) {
            Log.e("PDF Finder", "Directory is null or inaccessible: " + file.getAbsolutePath());
            return arrayList;
        }

        for (File siFile1 : files) {
            if (siFile1.isDirectory() && !siFile1.isHidden()) {
                arrayList.addAll(findPdf(siFile1));
            } else {
                if (siFile1.getName().endsWith(".pdf")) {
                    Log.d("PDF Finder", "Found PDF: " + siFile1.getName());
                    arrayList.add(siFile1);
                }
            }
        }
        return arrayList;
    }

    public void displayPdf() {
        recyclerView = findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        pdfList = new ArrayList<>();
        File directory = Environment.getExternalStorageDirectory();
        Log.d("PDF Finder", "Scanning directory: " + directory.getAbsolutePath());

        if (directory.exists() && directory.canRead()) {
            pdfList.addAll(findPdf(directory));
            Log.d("PDF Finder", "Total PDFs found: " + pdfList.size());
        } else {
            Log.e("PDF Finder", "Directory does not exist or is not readable: " + directory.getAbsolutePath());
        }

        if (pdfList.isEmpty()) {
            Log.w("PDF Finder", "No PDF files found.");
        }

        mainAdabter = new MainAdabter(this, pdfList, this);
        recyclerView.setAdapter(mainAdabter);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // تحقق من requestCode و resultCode
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data.getData() != null) {
                Uri selectedPdf = data.getData();
                String path = getRealPathFromURI(selectedPdf);
                if (path != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedPdf);
                        if (inputStream != null) {
                            File file = createTempFileFromInputStream(inputStream);
                            if (file.exists()) {
                                pdfList.add(file); // إضافة الملف إلى القائمة

                                // التأكد من تهيئة الـ Adapter
                                if (mainAdabter == null) {
                                    mainAdabter = new MainAdabter(this, pdfList, this);
                                    recyclerView.setAdapter(mainAdabter);
                                } else {
                                    mainAdabter.notifyDataSetChanged(); // تحديث الـ Adapter
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to read PDF", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    String path = getRealPathFromURI(uri);
                    if (path != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            if (inputStream != null) {
                                File file = createTempFileFromInputStream(inputStream);
                                if (file.exists()) {
                                    pdfList.add(file); // إضافة الملف إلى القائمة
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // تأكد من أن البيانات تم إضافتها بنجاح قبل تحديث الـ Adapter
                if (!pdfList.isEmpty()) {
                    if (mainAdabter == null) {
                        mainAdabter = new MainAdabter(this, pdfList, this);
                        recyclerView.setAdapter(mainAdabter);
                    } else {
                        mainAdabter.notifyDataSetChanged(); // تحديث الـ Adapter بعد إضافة الملفات
                    }
                }
            }
        } else {
            Toast.makeText(this, "No PDF file selected", Toast.LENGTH_SHORT).show();
        }
    }



    // تحويل InputStream إلى ملف مؤقت
    private File createTempFileFromInputStream(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("pdf_", ".pdf", getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
        return tempFile;
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return null;
    }

    @Override
    public void onPdfSelectListener(File f) {
        // التوجه إلى صفحة عرض الـ PDF بعد اختياره
        startActivity(new Intent(MainActivity.this, PdfActivity.class).putExtra("path", f.getAbsolutePath()));
    }
}
