package io.realm.datastorebenchmark.greendao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table "EMPLOYEE".
 */
public class Employee {

    private long id;
    private String name;
    private int age;
    private boolean hired;

    public Employee() {
    }

    public Employee(long id, String name, int age, boolean hired) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.hired = hired;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean getHired() {
        return hired;
    }

    public void setHired(boolean hired) {
        this.hired = hired;
    }

}
