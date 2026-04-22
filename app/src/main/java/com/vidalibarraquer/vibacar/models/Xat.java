package com.vidalibarraquer.vibacar.models;

public class Xat {

    private String id;
    private String viatgeId;
    private String conductorId;
    private String passatgerId;
    private String nomConductor;
    private String nomPassatger;
    private String origen;
    private String desti;
    private long sortidaMillis;
    private String darrerMissatge;
    private String darrerEmissorId;
    private long darreraActualitzacio;

    public Xat() {
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

    public String getNomConductor() {
        return nomConductor;
    }

    public void setNomConductor(String nomConductor) {
        this.nomConductor = nomConductor;
    }

    public String getNomPassatger() {
        return nomPassatger;
    }

    public void setNomPassatger(String nomPassatger) {
        this.nomPassatger = nomPassatger;
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

    public String getDarrerMissatge() {
        return darrerMissatge;
    }

    public void setDarrerMissatge(String darrerMissatge) {
        this.darrerMissatge = darrerMissatge;
    }

    public String getDarrerEmissorId() {
        return darrerEmissorId;
    }

    public void setDarrerEmissorId(String darrerEmissorId) {
        this.darrerEmissorId = darrerEmissorId;
    }

    public long getDarreraActualitzacio() {
        return darreraActualitzacio;
    }

    public void setDarreraActualitzacio(long darreraActualitzacio) {
        this.darreraActualitzacio = darreraActualitzacio;
    }
}
