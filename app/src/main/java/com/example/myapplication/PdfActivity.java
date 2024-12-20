package com.example.myapplication;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfActivity extends AppCompatActivity {
    String filepath = "";
    boolean isFocusMode = false;
    boolean isTimerRunning = false;
    boolean isDrawingMode = false;
    Handler handler = new Handler();
    Runnable timerRunnable;
    long remainingTime = 0;
    int hours = 0, minutes = 0, seconds = 0;

    private Map<Integer, String> notesMap = new HashMap<>(); // لتخزين الملاحظات المرتبطة بكل صفحة
    private PDFView pdfView;
    private TextView noteTextView;
    private List<PointF> drawingPoints = new ArrayList<>(); // قائمة لتخزين نقاط الرسم
    File file = new File(filepath);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        pdfView = findViewById(R.id.pdfView);
        noteTextView = findViewById(R.id.noteTextView);
        filepath = getIntent().getStringExtra("path");

        // تحميل الملاحظات من SharedPreferences عند بدء النشاط
        loadNotes();

         file = new File(filepath);
        pdfView.fromUri(Uri.fromFile(file))
                .spacing(2)  // ضبط المسافة بين الصفحات (بالبيكسل)
                .onLoad(nbPages -> {
                    // يمكن إضافة كود آخر هنا
                })
                .onPageChange((page, pageCount) -> {
                    updateNoteForCurrentPage(page); // تحديث الملاحظة المعروضة
                })
                .onDraw((canvas, pageWidth, pageHeight, displayedPage) -> {
                    if (isDrawingMode) {
                        drawOnPage(canvas, pageWidth, pageHeight, displayedPage);
                    }
                    addPageNumber(canvas, pageWidth, pageHeight, displayedPage);
                })
                .enableSwipe(true)  // تمكين السحب
                .load();

        Button focusButton = findViewById(R.id.focusButton);
        focusButton.setOnClickListener(v -> showTimePickerDialog());

        Button notesButton = findViewById(R.id.notesButton);
        notesButton.setOnClickListener(v -> showNotesDialog());

        Button drawButton = findViewById(R.id.drawButton);
        drawButton.setOnClickListener(v -> toggleDrawingMode());


    }

    private void setupDrawingTouchListener() {
        // وضع مستمع اللمس على الـ PDF
        pdfView.setOnTouchListener((v, event) -> {
            if (isDrawingMode) {
                // إذا كان وضع الرسم مفعل، نسمح بالرسم على الشاشة
                float x = event.getX();
                float y = event.getY();

                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    drawingPoints.add(new PointF(x, y));
                    pdfView.invalidate(); // إعادة رسم العرض
                }
            } else {
                // إذا لم يكن وضع الرسم مفعل، يجب تفعيل السكورل (التنقل بين الصفحات)
                return false; // السماح للسحب والتنقل بين الصفحات
            }
            return true;
        });
    }

    private void drawOnPage(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
        Paint paint = new Paint();
        paint.setColor(0xFF00FF00);  // لون الرسم
        paint.setStrokeWidth(5);     // سمك الخط

        // ارسم النقاط المخزنة في القائمة
        for (int i = 1; i < drawingPoints.size(); i++) {
            PointF start = drawingPoints.get(i - 1);
            PointF end = drawingPoints.get(i);
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);
        }
    }

    private void addPageNumber(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
        Paint paint = new Paint();
        paint.setColor(0xFF0000FF);  // لون النص (أزرق)
        paint.setTextSize(50);       // حجم النص

        // إضافة رقم الصفحة مع العدد الإجمالي
        int totalPages = pdfView.getPageCount();
        String pageNumberText = "Page " + (displayedPage + 1) + " of " + totalPages;

        // ضع النص في منتصف العرض السفلي
        float xPosition = (pageWidth - paint.measureText(pageNumberText)) / 2;
        float yPosition = pageHeight - 30;
        canvas.drawText(pageNumberText, xPosition, yPosition, paint);
    }

    private void showNotesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Note");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String note = input.getText().toString();
            int currentPage = pdfView.getCurrentPage();
            notesMap.put(currentPage, note); // حفظ الملاحظة مع الصفحة الحالية
            saveNotes(); // حفظ الملاحظات في SharedPreferences
            Toast.makeText(this, "Note Saved", Toast.LENGTH_SHORT).show();
            updateNoteForCurrentPage(currentPage); // تحديث الملاحظة في TextView
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateNoteForCurrentPage(int page) {
        String note = notesMap.get(page);
        if (note != null) {
            noteTextView.setText("Note: " + note);
        } else {
            noteTextView.setText("Note: No note added.");
        }
    }

    private void toggleDrawingMode() {
        isDrawingMode = !isDrawingMode;
        drawingPoints.clear(); // إعادة تعيين النقاط عند تفعيل/إلغاء وضع الرسم

        if (isDrawingMode) {
            Toast.makeText(this, "Drawing Mode Activated", Toast.LENGTH_SHORT).show();
            setupDrawingTouchListener(); // تفعيل مستمع اللمس للرسم
            pdfView.setSwipeEnabled(false); // تعطيل التمرير أثناء وضع الرسم
        } else {
            Toast.makeText(this, "Drawing Mode Deactivated", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PdfActivity.this, PdfActivity.class).putExtra("path", file.getAbsolutePath()));

        }
    }


    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                hours = hourOfDay;
                minutes = minute;
                remainingTime = (hours * 60 * 60 + minutes * 60) * 1000;
                startFocusModeWithTimer();
            }
        }, 0, 0, true);

        timePickerDialog.setOnCancelListener(dialog ->
                Toast.makeText(PdfActivity.this, "Time selection canceled", Toast.LENGTH_SHORT).show());
        timePickerDialog.show();
    }

    private void toggleFocusMode() {
        isFocusMode = !isFocusMode;
        Button focusButton = findViewById(R.id.focusButton);

        if (isFocusMode) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            focusButton.setVisibility(View.GONE);
            Toast.makeText(this, "Focus Mode Activated", Toast.LENGTH_SHORT).show();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            focusButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Focus Mode Deactivated", Toast.LENGTH_SHORT).show();
        }
    }

    private void startFocusModeWithTimer() {
        if (!isFocusMode) {
            toggleFocusMode();
        }
        isTimerRunning = true;
        handler.postDelayed(timerRunnable, 1000);
        Toast.makeText(this, "Timer started for " + hours + " hour(s) and " + minutes + " minute(s)", Toast.LENGTH_SHORT).show();
    }

    private void stopTimer() {
        isTimerRunning = false;
        handler.removeCallbacks(timerRunnable);
        Toast.makeText(this, "Timer stopped", Toast.LENGTH_SHORT).show();
        remainingTime = 0;
    }

    private void saveNotes() {
        // حفظ الملاحظات باستخدام SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("notes_pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<Integer, String> entry : notesMap.entrySet()) {
            editor.putString("note_" + entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    private void loadNotes() {
        // تحميل الملاحظات من SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("notes_pref", MODE_PRIVATE);
        for (int i = 0; i < pdfView.getPageCount(); i++) {
            String note = sharedPreferences.getString("note_" + i, null);
            if (note != null) {
                notesMap.put(i, note);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isFocusMode) {
            Toast.makeText(this, "Focus Mode is Active! You can't exit yet.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isFocusMode && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)) {
            return true; // Do nothing when focus mode is active
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }
}
