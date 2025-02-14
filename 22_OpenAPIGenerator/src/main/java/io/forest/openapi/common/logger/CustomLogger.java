package io.forest.openapi.common.logger;

import static java.util.Locale.UK;

import ch.qos.cal10n.MessageConveyor;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

public class CustomLogger {

    final static LocLoggerFactory locLoggerFactory = new LocLoggerFactory(new MessageConveyor(UK));

    public static LocLogger getLogger(Class<?> clazz) {
        return locLoggerFactory.getLocLogger(clazz);
    }

//    private CustomLogger(Class<?> clazz) {
//        locLoggerFactory = new LocLoggerFactory(new MessageConveyor(UK));
//    }
}
