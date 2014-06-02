/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.i18n;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author double-u
 */
public class I18N {

    private static Properties lang;

    public static String get(String key) {
        if (lang == null) {
            loadLanguages();
        }
        return lang.getProperty(key);
    }

    private static void loadLanguages() {
        if (lang != null) {
            lang.clear();
        } else {
            lang = new Properties();
        }

        try {
            lang.load(I18N.class.getResourceAsStream("/en-US.properties"));
        } catch (IOException ex) {
            Logger.getLogger(I18N.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
