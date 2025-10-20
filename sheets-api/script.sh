cd /Users/fonsoo/AndroidStudioProjects/mascotasApp/sheets-api

# usar SA del proyecto petcare-ac3c2 (recomendado)
export GOOGLE_APPLICATION_CREDENTIALS="/Users/fonsoo/AndroidStudioProjects/mascotasApp/sheets-api/functions/keys/noted-compass-475402-r1-061aa707a2e6.json"

# o si decides mantener el SA de noted-compas
export ALLOW_INSECURE_EMULATOR=1

cd functions && npm ci && cd ..
firebase emulators:start --only functions
