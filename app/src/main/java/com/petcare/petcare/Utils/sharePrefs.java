package com.petcare.petcare.Utils;

import android.content.Context;

import com.petcare.petcare.R;

public class sharePrefs {

    public static void setUserBdInicial(Context context, String points) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        preferences.edit().putString("userBdInicial", points).apply();
    }

    public static String getUserBdInicial(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        return preferences.getString("userBdInicial", "nao");
    }


    public static void setTipo(Context context, String points) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        preferences.edit().putString("tipo", points).apply();
    }

    public static String getTipo(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        return preferences.getString("tipo", "nao");
    }


    public static void setAvaliacoes(Context context, String points) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        preferences.edit().putString("liberaServicoInicial", points).apply();
    }

    public static String getAvaliacoes(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        return preferences.getString("liberaServicoInicial", "nao");
    }


    public static void setImgInicial(Context context, String points) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        preferences.edit().putString("imgInicial", points).apply();
    }

    public static String getImgInicial(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        return preferences.getString("imgInicial", "nao");
    }



    public static void setPetBdSeForEmpresarioInicial(Context context, String points) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        preferences.edit().putString("petBdSeForEmpresarioInicial", points).apply();
    }

    public static String getPetBdSeForEmpresarioInicial(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        return preferences.getString("petBdSeForEmpresarioInicial", "nao");
    }

    public static void setLembrete(Context context, String dataRemember, String nomeProduto, String bdDoPet) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        preferences.edit().putString("rememberDate", dataRemember).apply();
        preferences.edit().putString("remeberNomeProduto", nomeProduto).apply();
        preferences.edit().putString("rememberBdDoPEt", bdDoPet).apply();
    }

    public static String getLembreteData(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        return preferences.getString("rememberDate", "nao");
    }

    public static String getLembreteNomeProd(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        return preferences.getString("remeberNomeProduto", "nao");
    }

    public static String getLembreteBdDoPet(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.string.sharedpreferences), context.MODE_PRIVATE);
        return preferences.getString("rememberBdDoPEt", "nao");
    }

}

