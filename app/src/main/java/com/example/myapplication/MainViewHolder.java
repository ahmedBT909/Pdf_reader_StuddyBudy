package com.example.myapplication;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class MainViewHolder extends RecyclerView.ViewHolder {
    public TextView txtName;
    public CardView cardView;

    public MainViewHolder(@NonNull View view) {
        super(view);
        txtName = view.findViewById(R.id.pdf_name);
        cardView = view.findViewById(R.id.pdf_cardView);
    }
}
