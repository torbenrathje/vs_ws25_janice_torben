Mosquitto auf mehrern Rechnern im System verbinden:

Auf pc1: mosquitto.conf anpassen (im installationsordner: C:\Program Files\mosquitto\mosquitto.conf")

In die ersten Zeilen reinschreiben:
listener 1883
allow_anonymous true
-> Jeder im gleichen Netz kann sich verbinden (ohne Passwort)

IP des Rechners herausfinden Windows: ipconfig

Mosquitto mit conf in einem terminal starten (Terminal bleibt offen):
mosquitto -c "C:\Program Files\mosquitto\mosquitto.conf"

Testten:
Auf rechner 2:
mosquitto_sub -h 192.168.1.100 -t "ring/p0"

Auf rechner 1:
mosquitto_pub -h 192.168.1.100 -t "ring/p0" -m "Hallo"
Oder
mosquitto_pub -t "ring/p0" -m "Hallo"