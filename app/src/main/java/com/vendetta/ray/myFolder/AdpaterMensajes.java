package com.vendetta.ray.myFolder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.vendetta.ray.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class AdpaterMensajes extends RecyclerView.Adapter<HolderMensajes> {

    List<Mensaje> listMensajes = new ArrayList<>();
    private Context c;

    public AdpaterMensajes(Context c) {

        this.c = c;
    }

    public void addMensaje(DataSnapshot o){
        Mensaje m = new Mensaje(o.child("mensaje").getValue().toString(),
                o.child("nombre").getValue().toString(),
                o.child("fotoPerfil").getValue().toString(),
                o.child("type_mensaje").getValue().toString(),
                o.child("hora").getValue().toString());
        listMensajes.add(m);
        notifyItemInserted(listMensajes.size());
    }

    @NonNull
    @NotNull
    @Override
    public HolderMensajes onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {

       View v = LayoutInflater.from(c).inflate(R.layout.card_view_mensaje,parent,false);
        return new HolderMensajes(v);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull HolderMensajes holder, int position) {
    holder.getNombre().setText(listMensajes.get(position).getNombre());
    holder.getMensaje().setText(listMensajes.get(position).getMensaje());
    holder.getHora().setText(listMensajes.get(position).getHora());
    }

    @Override
    public int getItemCount() {
        return listMensajes.size();
    }


}
