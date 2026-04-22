package com.vidalibarraquer.vibacar.models;

public class Usuari {

    private String uid;
    private String nom;
    private String correu;
    private String telefon;
    private String rol;
    private String zona;
    private String horaSortidaHabitual;
    private String puntTrobadaHabitual;
    private String fotoUri;
    private String bio;
    private String modelCotxe;
    private String dataNaixement;
    private String sexe;
    private int placesHabituals;
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

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getFotoUri() {
        return fotoUri;
    }

    public void setFotoUri(String fotoUri) {
        this.fotoUri = fotoUri;
    }

    public String getHoraSortidaHabitual() {
        return horaSortidaHabitual;
    }

    public void setHoraSortidaHabitual(String horaSortidaHabitual) {
        this.horaSortidaHabitual = horaSortidaHabitual;
    }

    public String getPuntTrobadaHabitual() {
        return puntTrobadaHabitual;
    }

    public void setPuntTrobadaHabitual(String puntTrobadaHabitual) {
        this.puntTrobadaHabitual = puntTrobadaHabitual;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getModelCotxe() {
        return modelCotxe;
    }

    public void setModelCotxe(String modelCotxe) {
        this.modelCotxe = modelCotxe;
    }

    public String getDataNaixement() {
        return dataNaixement;
    }

    public void setDataNaixement(String dataNaixement) {
        this.dataNaixement = dataNaixement;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public int getPlacesHabituals() {
        return placesHabituals;
    }

    public void setPlacesHabituals(int placesHabituals) {
        this.placesHabituals = placesHabituals;
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
