package com.vidalibarraquer.vibacar.models;

@SuppressWarnings("SpellCheckingInspection")
public class Usuari {

    private String uid;
    private String nom;
    private String correu;
    private String telefon;
    private String rol;
    private String fotoUri;
    private String bio;
    private boolean perfilCompletat;
    private double valoracioMitjana;
    private long totalValoracions;
    private String idioma;

    public Usuari() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCorreu() {
        return correu;
    }

    public void setCorreu(String correu) {
        this.correu = correu;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getFotoUri() {
        return fotoUri;
    }

    public void setFotoUri(String fotoUri) {
        this.fotoUri = fotoUri;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isPerfilCompletat() {
        return perfilCompletat;
    }

    public void setPerfilCompletat(boolean perfilCompletat) {
        this.perfilCompletat = perfilCompletat;
    }

    public double getValoracioMitjana() {
        return valoracioMitjana;
    }

    public void setValoracioMitjana(double valoracioMitjana) {
        this.valoracioMitjana = valoracioMitjana;
    }

    public long getTotalValoracions() {
        return totalValoracions;
    }

    public void setTotalValoracions(long totalValoracions) {
        this.totalValoracions = totalValoracions;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

}
