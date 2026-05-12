# Dades demo ViBaCar

Aquest seed prepara una demo per a la presentacio del 14 de maig de 2026 amb tres usuaries reals de Firebase Auth.

## Que crea

- 3 perfils complets a `usuaris`.
- 3 viatges:
  - un viatge acceptat amb xat obert,
  - una sollicitud pendent,
  - un viatge passat per demostrar valoracions.
- Reserves acceptades i pendents.
- Missatges dins d'un xat.
- Notificacions no llegides.
- Una valoracio guardada al perfil d'una usuaria.

## Abans d'executar-lo

1. Crea o inicia sessio amb els 3 comptes demo a l'app.
2. Ves a Firebase Console > Authentication > Users i copia els 3 UID.
3. Ves a Firebase Console > Project settings > Service accounts.
4. Genera una clau privada de Firebase Admin i desa-la fora del repo o com `serviceAccountKey.json` local.

No pugis mai `serviceAccountKey.json` a Git.

## Instal·lacio

Si no tens `firebase-admin` instal·lat al projecte:

```powershell
npm init -y
npm install firebase-admin
```

## Execucio en PowerShell

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\ruta\serviceAccountKey.json"
$env:FIREBASE_PROJECT_ID="pid-damingo"

$env:DEMO_UID_1="uid_de_la_dijaa"
$env:DEMO_UID_2="uid_companya_1"
$env:DEMO_UID_3="uid_companya_2"

$env:DEMO_NOM_1="Dijaa Benali"
$env:DEMO_NOM_2="Laia Soler"
$env:DEMO_NOM_3="Nora Garcia"

node scripts/seed-demo-data.js
```

## Guio curt per ensenyar-ho

1. Entra com la usuaria 2: veuras una reserva acceptada, xat i notificacio.
2. Entra com la usuaria 1: veuras una sollicitud pendent i podras acceptar-la.
3. Entra com la usuaria 3: ves al perfil i mostra un viatge passat per valorar.
4. Obre el perfil public de la usuaria 2 per veure comentaris i valoracions persistents.

Les dades tenen ids `demo_...`, per tant es poden identificar facilment a Firestore.
