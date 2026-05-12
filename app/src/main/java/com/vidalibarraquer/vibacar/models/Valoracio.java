package com.vidalibarraquer.vibacar.models;

public class Valoracio {

    private String id;
    private String autorId;
    private String autorNom;
    private String valoratId;
    private String reservaId;
    private String viatgeId;
    private String origen;
    private String desti;
    private String comentari;
    private float puntuacio;
    private long dataMillis;

    public Valoracio() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAutorId() { return autorId; }
    public void setAutorId(String autorId) { this.autorId = autorId; }
    public String getAutorNom() { return autorNom; }
    public void setAutorNom(String autorNom) { this.autorNom = autorNom; }
    public String getValoratId() { return valoratId; }
    public void setValoratId(String valoratId) { this.valoratId = valoratId; }
    public String getReservaId() { return reservaId; }
    public void setReservaId(String reservaId) { this.reservaId = reservaId; }
    public String getViatgeId() { return viatgeId; }
    public void setViatgeId(String viatgeId) { this.viatgeId = viatgeId; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDesti() { return desti; }
    public void setDesti(String desti) { this.desti = desti; }
    public String getComentari() { return comentari; }
    public void setComentari(String comentari) { this.comentari = comentari; }
    public float getPuntuacio() { return puntuacio; }
    public void setPuntuacio(float puntuacio) { this.puntuacio = puntuacio; }
    public long getDataMillis() { return dataMillis; }
    public void setDataMillis(long dataMillis) { this.dataMillis = dataMillis; }
}
