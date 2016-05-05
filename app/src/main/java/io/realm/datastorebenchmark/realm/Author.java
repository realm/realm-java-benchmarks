package io.realm.datastorebenchmark.realm;

import io.realm.RealmObject;

public class Author extends RealmObject {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
