# Firebase Auth + Firestore per ViBaCar

## 1. Crear el projecte Firebase

1. Entra a Firebase Console i crea un projecte nou.
2. Afegeix una app Android amb el package `com.vidalibarraquer.vibacar`.
3. Descarrega el fitxer `google-services.json`.
4. Copia'l a `app/google-services.json`.

## 2. Activar Authentication

1. Ves a `Build > Authentication`.
2. Prem `Get started`.
3. Activa el provider `Email/Password`.
4. Guarda els canvis.

## 3. Activar Firestore

1. Ves a `Build > Firestore Database`.
2. Prem `Create database`.
3. Escull `Test mode` per desenvolupar.
4. Selecciona la regio mes propera.

## 4. Estructura minima guardada a Firestore

Colleccio: `users`

Document ID:
- `uid` de Firebase Authentication

Camps:
- `uid`
- `fullName`
- `email`
- `role`
- `meetingZone`
- `emailVerified`
- `createdAt`
- `updatedAt`
- `lastLoginAt`

## 5. Regles inicials recomanades

Per comencar, una base segura i simple per la colleccio `users` es aquesta:

```txt
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow create: if request.auth != null
                    && request.auth.uid == userId;

      allow read, update: if request.auth != null
                          && request.auth.uid == userId;

      allow delete: if false;
    }
  }
}
```

## 6. Prova manual recomanada

1. Registra un usuari amb un correu `@vidalibarraquer.net`.
2. Comprova que arriba el correu de verificacio.
3. Intenta entrar abans de verificar:
   l'app ha de bloquejar l'acces.
4. Verifica el correu.
5. Torna a l'app i prem `Ja l'he verificat`.
6. Comprova a Firestore que s'ha creat el document a `users/{uid}`.

## 7. Seguent pas del projecte

Quan aquest punt funcioni, el cami natural del projecte es:

1. Crear la colleccio `trips`.
2. Crear la colleccio `chatRooms` o `messages`.
3. Afegir `FusedLocationProviderClient` per ubicacio.
4. Afegir mapa amb Google Maps SDK.
5. Filtrar trajectes per zona de sortida i rol.
