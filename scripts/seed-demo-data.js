const admin = require("firebase-admin");

const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS || "./serviceAccountKey.json";
const projectId = process.env.FIREBASE_PROJECT_ID || "pid-damingo";

const required = ["DEMO_UID_1", "DEMO_UID_2", "DEMO_UID_3"];
const missing = required.filter((name) => !process.env[name]);
if (missing.length > 0) {
  console.error(`Falten variables: ${missing.join(", ")}`);
  console.error("Exemple:");
  console.error("$env:GOOGLE_APPLICATION_CREDENTIALS='C:\\\\ruta\\\\serviceAccountKey.json'");
  console.error("$env:DEMO_UID_1='uid_de_la_dijaa'");
  console.error("$env:DEMO_UID_2='uid_companya_1'");
  console.error("$env:DEMO_UID_3='uid_companya_2'");
  console.error("node scripts/seed-demo-data.js");
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(serviceAccountPath)),
  projectId,
});

const db = admin.firestore();

const demoUsers = [
  {
    uid: process.env.DEMO_UID_1,
    nom: process.env.DEMO_NOM_1 || "Dijaa Benali",
    correu: process.env.DEMO_EMAIL_1 || "dijaa.demo@vidalibarraquer.net",
    bio: "Condueixo entre casa i el centre gairebe cada mati. M'agrada deixar-ho tot clar pel xat.",
    fotoUri: "",
    valoracioMitjana: 4.8,
    totalValoracions: 5,
  },
  {
    uid: process.env.DEMO_UID_2,
    nom: process.env.DEMO_NOM_2 || "Laia Soler",
    correu: process.env.DEMO_EMAIL_2 || "laia.demo@vidalibarraquer.net",
    bio: "Passatgera puntual. Sempre aviso si hi ha algun canvi.",
    fotoUri: "",
    valoracioMitjana: 4.6,
    totalValoracions: 4,
  },
  {
    uid: process.env.DEMO_UID_3,
    nom: process.env.DEMO_NOM_3 || "Nora Garcia",
    correu: process.env.DEMO_EMAIL_3 || "nora.demo@vidalibarraquer.net",
    bio: "Comparteixo trajectes quan surto de practiques i torno cap al centre.",
    fotoUri: "",
    valoracioMitjana: 4.9,
    totalValoracions: 6,
  },
];

const day = (isoDate, hour, minute) => {
  const [year, month, date] = isoDate.split("-").map(Number);
  return new Date(year, month - 1, date, hour, minute, 0, 0).getTime();
};

const routes = {
  barcelonaCentre: {
    origen: "Estacio de Sants, Barcelona",
    desti: "Institut Vidal i Barraquer, Tarragona",
    origenLat: 41.3791,
    origenLng: 2.1402,
    destiLat: 41.1189,
    destiLng: 1.2445,
  },
  reusCentre: {
    origen: "Reus Centre",
    desti: "Institut Vidal i Barraquer, Tarragona",
    origenLat: 41.1548,
    origenLng: 1.1087,
    destiLat: 41.1189,
    destiLng: 1.2445,
  },
  salouCentre: {
    origen: "Salou",
    desti: "Institut Vidal i Barraquer, Tarragona",
    origenLat: 41.0772,
    origenLng: 1.1416,
    destiLat: 41.1189,
    destiLng: 1.2445,
  },
};

function user(uid) {
  return demoUsers.find((item) => item.uid === uid);
}

function trip(id, conductorUid, route, sortida, arribada, placesDisponibles, estat, aportacio) {
  const conductor = user(conductorUid);
  return {
    id,
    data: {
      conductorId: conductorUid,
      conductorNom: conductor.nom,
      conductorFotoUri: conductor.fotoUri,
      modelCotxeConductor: "Toyota Yaris",
      colorCotxeConductor: "Blanc",
      conductorValoracio: conductor.valoracioMitjana,
      conductorValoracions: conductor.totalValoracions,
      origen: route.origen,
      desti: route.desti,
      zonaSortida: route.origen,
      origenLat: route.origenLat,
      origenLng: route.origenLng,
      destiLat: route.destiLat,
      destiLng: route.destiLng,
      sortidaMillis: sortida,
      arribadaMillis: arribada,
      placesTotals: 3,
      placesDisponibles,
      aportacio,
      observacions: "Demo presentacio: punt de trobada confirmat pel xat.",
      estat,
    },
  };
}

const trips = [
  trip("demo_viatge_acceptat", demoUsers[0].uid, routes.barcelonaCentre, day("2026-05-14", 7, 20), day("2026-05-14", 8, 35), 1, "disponible", 4.5),
  trip("demo_viatge_pendent", demoUsers[2].uid, routes.reusCentre, day("2026-05-14", 7, 45), day("2026-05-14", 8, 20), 2, "disponible", 2.0),
  trip("demo_viatge_passat", demoUsers[1].uid, routes.salouCentre, day("2026-05-10", 7, 30), day("2026-05-10", 8, 10), 1, "completat", 2.5),
];

const reservations = [
  {
    id: `demo_viatge_acceptat_${demoUsers[1].uid}`,
    data: {
      viatgeId: "demo_viatge_acceptat",
      conductorId: demoUsers[0].uid,
      passatgerId: demoUsers[1].uid,
      passatgerNom: demoUsers[1].nom,
      conductorNom: demoUsers[0].nom,
      origen: routes.barcelonaCentre.origen,
      desti: routes.barcelonaCentre.desti,
      sortidaMillis: day("2026-05-14", 7, 20),
      estat: "acceptada",
      acceptadaMillis: day("2026-05-12", 18, 0),
      valorada: false,
      puntuacio: 0,
      conductorValorada: false,
      conductorPuntuacio: 0,
    },
  },
  {
    id: `demo_viatge_pendent_${demoUsers[0].uid}`,
    data: {
      viatgeId: "demo_viatge_pendent",
      conductorId: demoUsers[2].uid,
      passatgerId: demoUsers[0].uid,
      passatgerNom: demoUsers[0].nom,
      conductorNom: demoUsers[2].nom,
      origen: routes.reusCentre.origen,
      desti: routes.reusCentre.desti,
      sortidaMillis: day("2026-05-14", 7, 45),
      estat: "pendent",
      valorada: false,
      puntuacio: 0,
      conductorValorada: false,
      conductorPuntuacio: 0,
    },
  },
  {
    id: `demo_viatge_passat_${demoUsers[2].uid}`,
    data: {
      viatgeId: "demo_viatge_passat",
      conductorId: demoUsers[1].uid,
      passatgerId: demoUsers[2].uid,
      passatgerNom: demoUsers[2].nom,
      conductorNom: demoUsers[1].nom,
      origen: routes.salouCentre.origen,
      desti: routes.salouCentre.desti,
      sortidaMillis: day("2026-05-10", 7, 30),
      estat: "acceptada",
      valorada: true,
      puntuacio: 5,
      comentari: "Trajecte molt comode i puntual.",
      conductorValorada: false,
      conductorPuntuacio: 0,
    },
  },
];

const chatId = `xat_${[demoUsers[0].uid, demoUsers[1].uid].sort().join("_")}`;
const now = Date.now();

async function setDoc(path, data, batch) {
  batch.set(db.doc(path), data, { merge: true });
}

async function seed() {
  const batch = db.batch();

  demoUsers.forEach((item) => {
    setDoc(`usuaris/${item.uid}`, {
      uid: item.uid,
      nom: item.nom,
      correu: item.correu,
      telefon: "",
      rol: "usuari",
      fotoUri: item.fotoUri,
      bio: item.bio,
      perfilCompletat: true,
      emailVerified: true,
      idioma: "ca",
      valoracioMitjana: item.valoracioMitjana,
      totalValoracions: item.totalValoracions,
    }, batch);
  });

  trips.forEach((item) => setDoc(`viatges/${item.id}`, item.data, batch));
  reservations.forEach((item) => setDoc(`reserves/${item.id}`, item.data, batch));

  setDoc(`xats/${chatId}`, {
    viatgeId: "demo_viatge_acceptat",
    conductorId: demoUsers[0].uid,
    passatgerId: demoUsers[1].uid,
    nomConductor: demoUsers[0].nom,
    nomPassatger: demoUsers[1].nom,
    origen: routes.barcelonaCentre.origen,
    desti: routes.barcelonaCentre.desti,
    sortidaMillis: day("2026-05-14", 7, 20),
    darreraActualitzacio: now,
    darrerMissatge: "Perfecte, ens veiem a Sants a les 7:15.",
    darrerEmissorId: demoUsers[0].uid,
  }, batch);

  setDoc(`xats/${chatId}/missatges/demo_1`, {
    emissorId: demoUsers[1].uid,
    text: "Hola! Encara tens plaça pel viatge de dijous?",
    dataMillis: now - 600000,
  }, batch);
  setDoc(`xats/${chatId}/missatges/demo_2`, {
    emissorId: demoUsers[0].uid,
    text: "Si, t'he acceptat la reserva. Sortim de Sants.",
    dataMillis: now - 420000,
  }, batch);
  setDoc(`xats/${chatId}/missatges/demo_3`, {
    emissorId: demoUsers[1].uid,
    text: "Perfecte, moltes gracies!",
    dataMillis: now - 300000,
  }, batch);
  setDoc(`xats/${chatId}/missatges/demo_4`, {
    emissorId: demoUsers[0].uid,
    text: "Perfecte, ens veiem a Sants a les 7:15.",
    dataMillis: now - 180000,
  }, batch);

  setDoc(`notificacions/${demoUsers[1].uid}/items/demo_reserva_acceptada`, {
    tipus: "reserva_acceptada",
    text: "La teva reserva Estacio de Sants, Barcelona -> Institut Vidal i Barraquer, Tarragona ha estat acceptada. Ja pots obrir el xat.",
    llegida: false,
    dataMillis: now - 120000,
    referenciaId: chatId,
  }, batch);
  setDoc(`notificacions/${demoUsers[0].uid}/items/demo_nova_reserva`, {
    tipus: "nova_reserva",
    text: "Nora Garcia ha demanat plaça al teu viatge de dijous.",
    llegida: false,
    dataMillis: now - 90000,
    referenciaId: "demo_viatge_pendent",
  }, batch);
  setDoc(`notificacions/${demoUsers[1].uid}/items/demo_missatge`, {
    tipus: "nou_missatge",
    text: "Dijaa Benali t'ha enviat un missatge.",
    llegida: false,
    dataMillis: now - 60000,
    referenciaId: chatId,
  }, batch);

  setDoc(`usuaris/${demoUsers[1].uid}/valoracions/demo_viatge_passat_${demoUsers[2].uid}`, {
    autorId: demoUsers[2].uid,
    autorNom: demoUsers[2].nom,
    valoratId: demoUsers[1].uid,
    reservaId: `demo_viatge_passat_${demoUsers[2].uid}`,
    viatgeId: "demo_viatge_passat",
    origen: routes.salouCentre.origen,
    desti: routes.salouCentre.desti,
    puntuacio: 5,
    comentari: "Trajecte molt comode i puntual.",
    dataMillis: day("2026-05-10", 18, 30),
  }, batch);

  await batch.commit();
  console.log("Dades demo creades correctament.");
  console.log(`Xat demo: ${chatId}`);
}

seed()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
