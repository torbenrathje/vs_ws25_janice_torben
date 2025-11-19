package org.example;

import java.util.NoSuchElementException;

public interface Datastore {
    public void write(int index, String data);
    public String read(int index) throws NoSuchElementException;
}
