package com.buahbatu.toyotasalesman;

import io.realm.RealmObject;

public class ErrorLog extends RealmObject {

    private String date;
    private String message;

    public String getDate() {
        return date;
    }

    public ErrorLog setDate(String date) {
        this.date = date;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ErrorLog setMessage(String message) {
        this.message = message;
        return this;
    }
}
