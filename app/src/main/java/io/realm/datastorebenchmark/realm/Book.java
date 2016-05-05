package io.realm.datastorebenchmark.realm;

import io.realm.RealmObject;

public class Book extends RealmObject {

    private Author author;
    private String name;

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
