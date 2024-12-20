package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class MainAdabter extends RecyclerView.Adapter<MainViewHolder> {
    private final Context context;
    private final List<File> pdfFiles;
    private OnPdfSelectListener listener;

    public MainAdabter(Context context, List<File> pdfFiles, OnPdfSelectListener listener) {
        this.context = context;
        this.pdfFiles = pdfFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MainViewHolder(LayoutInflater.from(context).inflate(R.layout.rec_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.txtName.setText(pdfFiles.get(position).getName());
        holder.txtName.setSelected(true);
        holder.cardView.setOnClickListener(view -> listener.onPdfSelectListener(pdfFiles.get(position)));
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }
}
