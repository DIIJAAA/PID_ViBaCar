package com.vidalibarraquer.vibacar.models;

public class Reserva {

    private String id;
    private String viatgeId;
    private String conductorId;
    private String passatgerId;
    private String passatgerNom;
    private String conductorNom;
    private String passatgerFotoUri;
    private String conductorFotoUri;
    private String origen;
    private String desti;
    private long sortidaMillis;
    private String estat;
    private boolean valorada;
    private float puntuacio;
    private boolean conductorValorada;
    private float conductorPuntuacio;
    private transient boolean socConductor;

    public Reserva() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getViatgeId() {
        return viatgeId;
    }

    public void setViatgeId(String viatgeId) {
        this.viatgeId = viatgeId;
    }

    public String getConductorId() {
        return conductorId;
    }

    public void setConductorId(String conductorId) {
        this.conductorId = conductorId;
    }

    public String getPassatgerId() {
        return passatgerId;
    }

    public void setPassatgerId(String passatgerId) {
        this.passatgerId = passatgerId;
    }

    public String getConductorNom() {
        return conductorNom;
    }

    public void setConductorNom(String conductorNom) {
        this.conductorNom = conductorNom;
    }

    public String getPassatgerNom() {
        return passatgerNom;
    }

    public void setPassatgerNom(String passatgerNom) {
        this.passatgerNom = passatgerNom;
    }

    public String getPassatgerFotoUri() {
        return passatgerFotoUri;
    }

    public void setPassatgerFotoUri(String passatgerFotoUri) {
        this.passatgerFotoUri = passatgerFotoUri;
    }

    public String getConductorFotoUri() {
        return conductorFotoUri;
    }

    public void setConductorFotoUri(String conductorFotoUri) {
        this.conductorFotoUri = conductorFotoUri;
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

    public long getSortidaMillis() {
        return sortidaMillis;
    }

    public void setSortidaMillis(long sortidaMillis) {
        this.sortidaMillis = sortidaMillis;
    }

    public String getEstat() {
        return estat;
    }

    public void setEstat(String estat) {
        this.estat = estat;
    }

    public boolean isValorada() {
        return valorada;
    }

    public void setValorada(boolean valorada) {
        this.valorada = valorada;
    }

    public float getPuntuacio() {
        return puntuacio;
    }

    public void setPuntuacio(float puntuacio) {
        this.puntuacio = puntuacio;
    }

    public boolean isConductorValorada() {
        return conductorValorada;
    }

    public void setConductorValorada(boolean conductorValorada) {
        this.conductorValorada = conductorValorada;
    }

    public float getConductorPuntuacio() {
        return conductorPuntuacio;
    }

    public void setConductorPuntuacio(float conductorPuntuacio) {
        this.conductorPuntuacio = conductorPuntuacio;
    }

    public boolean isSocConductor() {
        return socConductor;
    }

    public void setSocConductor(boolean socConductor) {
        this.socConductor = socConductor;
    }
}
