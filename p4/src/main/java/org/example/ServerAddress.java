package org.example;

/**
 * Repräsentiert einen Server in der Replikationsumgebung.
 * Dient als einfache Datenstruktur (immutable Record) für IP + Port.
 */
public record ServerAddress(String ip, int port) {}
