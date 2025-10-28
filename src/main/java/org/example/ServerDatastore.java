package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class ServerDatastore implements Datastore {

    private Map<Integer, String> store;

    public ServerDatastore() {
        store = new HashMap<>();
    }

    @Override
    public void write(int index, String data) {
        store.put(index, data);
    }

    @Override
    public String read(int index) throws NoSuchElementException {
        if (!store.containsKey(index)) {
            throw new NoSuchElementException();
        }
        return store.get(index);
    }
}
