package org.maboroshi.partyanimals.util;

import java.util.function.BooleanSupplier;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class Log {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static ComponentLogger logger;
    private static BooleanSupplier debugEnabled = () -> false;

    public static void init(ComponentLogger componentLogger, BooleanSupplier debugCondition) {
        logger = componentLogger;
        debugEnabled = debugCondition;
    }

    public static void info(String message, TagResolver... resolvers) {
        if (logger != null) {
            logger.info(MINI_MESSAGE.deserialize(message, resolvers));
        }
    }

    public static void warn(String message, TagResolver... resolvers) {
        if (logger != null) {
            logger.warn(MINI_MESSAGE.deserialize(message, resolvers));
        }
    }

    public static void error(String message, TagResolver... resolvers) {
        if (logger != null) {
            logger.error(MINI_MESSAGE.deserialize(message, resolvers));
        }
    }

    public static void debug(String message, TagResolver... resolvers) {
        if (logger != null && debugEnabled.getAsBoolean()) {
            logger.info(MINI_MESSAGE.deserialize("<gray>[DEBUG]</gray> <white>" + message + "</white>", resolvers));
        }
    }
}
