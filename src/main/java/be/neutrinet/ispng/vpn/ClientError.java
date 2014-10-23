/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn;

import be.neutrinet.ispng.i18n.I18N;

/**
 *
 * @author wannes
 */
public class ClientError {

    // Message identifier. Can never be null
    public String errorKey;
    // Message clearly explaining the problem. Can never be null
    public String message;
    // Link to URL containing more info. Can be null
    public String url;
    // Exception, if any
    public transient Throwable throwable;

    public ClientError(String key, String message, String url) {
        this.errorKey = key;
        this.message = message;
        this.url = url;
    }

    public ClientError(String key, Throwable throwable) {
        this.errorKey = key;
        this.throwable = throwable;
        this.message = throwable.getMessage();
    }

    public ClientError(String key, String message) {
        this.errorKey = key;
        this.message = message;
    }

    public ClientError(String key) {
        this.errorKey = key;
        this.message = I18N.get(key);
    }
}
