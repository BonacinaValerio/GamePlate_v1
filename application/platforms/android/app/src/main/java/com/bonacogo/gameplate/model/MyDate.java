package com.bonacogo.gameplate.model;

import java.io.Serializable;

public class MyDate implements Serializable {
    private Long _nanoseconds;
    private Long _seconds;

    public MyDate(Long _nanoseconds, Long _seconds) {
        this._nanoseconds = _nanoseconds;
        this._seconds = _seconds;
    }

    public Long getNanoseconds() {
        return _nanoseconds;
    }

    public Long getSeconds() {
        return _seconds;
    }
}
