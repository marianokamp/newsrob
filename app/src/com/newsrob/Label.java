package com.newsrob;

public class Label {

    private long id = -1;
    private String name;
    private int order;

    public Label() {
    }

    Label(long id) {
        this.id = id;
    }

    String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    long getId() {
        return id;
    }

    void setId(long id) {
        this.id = id;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "Label " + getName() + " (" + getId() + ")";
    }
}
