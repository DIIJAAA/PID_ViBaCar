package com.vidalibarraquer.vibacar.models;

public class Notificacio {

    public static final String TIPUS_NOVA_RESERVA = "nova_reserva";
    public static final String TIPUS_RESERVA_ACCEPTADA = "reserva_acceptada";
    public static final String TIPUS_RESERVA_REBUTJADA = "reserva_rebutjada";
    public static final String TIPUS_NOU_MISSATGE = "nou_missatge";
    public static final String TIPUS_VIATGE_CANCELLAT = "viatge_cancellat";
    public static final String TIPUS_VIATGE_COMPLETAT = "viatge_completat";
    public static final String TIPUS_RECORDATORI_VIATGE = "recordatori_viatge";

    private String id;
    private String tipus;
    private String text;
    private boolean llegida;
    private long dataMillis;
    private String referenciaId;

    public Notificacio() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTipus() { return tipus; }
    public void setTipus(String tipus) { this.tipus = tipus; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isLlegida() { return llegida; }
    public void setLlegida(boolean llegida) { this.llegida = llegida; }
    public long getDataMillis() { return dataMillis; }
    public void setDataMillis(long dataMillis) { this.dataMillis = dataMillis; }
    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }
}
