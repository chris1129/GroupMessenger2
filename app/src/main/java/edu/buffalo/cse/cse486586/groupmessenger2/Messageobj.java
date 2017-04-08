package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by sheng-yungcheng on 3/16/17.
 */

public class Messageobj implements Serializable {
    String message;
    int counter;
    int port;
    String action;
    Messageobj(String msg,int cnt, int prt, String act){
        this.message=msg;
        this.counter=cnt;
        this.port=prt;
        this.action=act;

    }

}

