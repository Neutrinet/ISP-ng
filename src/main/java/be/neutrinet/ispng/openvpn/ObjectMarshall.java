/*
 * ObjectMarshall.java
 * Copyright (C) Apr 6, 2014 Wannes De Smet
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package be.neutrinet.ispng.openvpn;

import net.wgr.core.ReflectionUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

/**
 *
 * @author wannes
 */
public class ObjectMarshall {

    public static <T> T fuzzyBuild(Map<String, String> input, Class<T> type) {
        try {
            T instance = type.newInstance();
            for (Field f : ReflectionUtils.getAllFields(type)) {
                String key = f.getName();
                if (f.isAnnotationPresent(Alias.class)) {
                    Alias alias = f.getAnnotation(Alias.class);
                    key = alias.key();
                }

                f.setAccessible(true);
                String value = input.get(key);
                if (value == null) {
                    // try underscore key
                    key = key.replaceAll("(\\p{Ll})(\\p{Lu})", "$1_$2").toLowerCase();
                    value = input.get(key);
                }

                // if value still is null, bug out
                if (value == null) {
                    continue;
                }

                try {
                    switch (f.getType().getCanonicalName()) {
                        case "java.lang.String":
                            f.set(instance, value);
                            break;
                        case "int":
                        case "java.lang.Integer":
                            f.set(instance, Integer.parseInt(value));
                            break;
                        case "long":
                        case "java.lang.Long":
                            f.set(instance, Long.parseLong(value));
                            break;
                        case "boolean":
                        case "java.lang.Boolean":
                            f.set(instance, Boolean.parseBoolean(value) || value.equals("1"));
                            break;
                        default:
                            //poop;
                            break;
                    }
                } catch (NumberFormatException ex) {
                    Logger.getLogger(ObjectMarshall.class).error("Failed to map " + key, ex);
                }
            }

            return instance;
        } catch (IllegalAccessException | InstantiationException ex) {
            Logger.getLogger(ObjectMarshall.class).error("Failed to reflect properly", ex);
            return null;
        }
    }
}
