package com.vidalibarraquer.vibacar.models;

public class Viatge {

    private String id;
    private String conductorId;
    private String conductorNom;
    private String conductorFotoUri;
    private String modelCotxeConductor;
    private String colorCotxeConductor;
    private double conductorValoracio;
    private long conductorValoracions;
    private String origen;
    private String desti;
    private String zonaSortida;
    private long sortidaMillis;
    private long arribadaMillis;
    private int placesTotals;
    private int placesDisponibles;
    private double aportacio;
    private String observacions;
    private String estat;

    public Viatge() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConductorId() {
        return conductorId;
    }

    public void setConductorId(String conductorId) {
        this.conductorId = conductorId;
    }

    public String getConductorNom() {
        return conductorNom;
    }

    public void setConductorNom(String conductorNom) {
        this.conductorNom = conductorNom;
    }

    public String getConductorFotoUri() {
        return conductorFotoUri;
    }

    public void setConductorFotoUri(String conductorFotoUri) {
        this.conductorFotoUri = conductorFotoUri;
    }

    public String getModelCotxeConductor() {
        return modelCotxeConductor;
    }

    public void setModelCotxeConductor(String modelCotxeConductor) {
        this.modelCotxeConductor = modelCotxeConductor;
    }

    public String getColorCotxeConductor() {
        return colorCotxeConductor;
    }

    public void setColorCotxeConductor(String colorCotxeConductor) {
        this.colorCotxeConductor = colorCotxeConductor;
    }

    public double getConductorValoracio() {
        return conductorValoracio;
    }

    public void setConductorValoracio(double conductorValoracio) {
        this.conductorValoracio = conductorValoracio;
    }

    public long getConductorValoracions() {
        return conductorValoracions;
    }

    public void setConductorValoracions(long conductorValoracions) {
        this.conductorValoracions = conductorValoracions;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDesti() {
        return desti;
    }

    public void setDesti(String desti) {
        this.desti = desti;
    }

    public String getZonaSortida() {
        return zonaSortida;
    }

    public void setZonaSortida(String zonaSortida) {
        this.zonaSortida = zonaSortida;
    }

    public long getSortidaMillis() {
        return sortidaMillis;
    }

    public void setSortidaMillis(long sortidaMillis) {
        this.sortidaMillis = sortidaMillis;
    }

    public long getArribadaMillis() {
        return arribadaMillis;
    }

    public void setArribadaMillis(long arribadaMillis) {
        this.arribadaMillis = arribadaMillis;
    }

    public int getPlacesTotals() {
        return placesTotals;
    }

    public void setPlacesTotals(int placesTotals) {
        this.placesTotals = placesTotals;
    }

    public int getPlacesDisponibles() {
        return placesDisponibles;
    }

    public void setPlacesDisponibles(int placesDisponibles) {
        this.placesDisponibles = placesDisponibles;
    }

    public double getAportacio() {
        return aportacio;
    }

    public void setAportacio(double aportacio) {
        this.aportacio = aportacio;
    }

    public String getObservacions() {
        return observacions;
    }

    public void setObservacions(String observacions) {
        this.observacions = observacions;
    }

    public String getEstat() {
        return estat;
    }

    public void setEstat(String estat) {
        this.estat = estat;
    }
}
