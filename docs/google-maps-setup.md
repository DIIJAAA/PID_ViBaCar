# Google Maps a ViBaCar

Perque el mapa es vegi a l'app, cal posar la clau de Google Maps al fitxer `local.properties` del projecte:

```properties
MAPS_API_KEY=la_teva_clau_aqui
```

Passos recomanats:

1. A Google Cloud Console, activa `Maps SDK for Android`.
2. Crea una API key nova.
3. Limita la clau al package `com.vidalibarraquer.vibacar`.
4. Afegeix la clau a `local.properties`.
5. Fes `Sync Project with Gradle Files` i torna a executar l'app.

Sense aquesta clau, l'app compila igual, pero el mapa no es dibuixara correctament.
