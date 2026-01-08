@echo off
REM Kompiliere alle Klassen
javac -d bin src\main\java\org\example\*.java

REM Starte Clients in eigenen Fenstern
start cmd /k java -cp "bin" org.example.TerminalClient client1 127.0.0.1 10000
start cmd /k java -cp "bin" org.example.TerminalClient client2 127.0.0.1 10001