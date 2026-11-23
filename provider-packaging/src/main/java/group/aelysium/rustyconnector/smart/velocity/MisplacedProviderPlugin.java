package group.aelysium.rustyconnector.smart.velocity;

import com.google.inject.Inject;
import org.slf4j.Logger;

public final class MisplacedProviderPlugin {
    @Inject
    public MisplacedProviderPlugin(Logger logger) {
        logger.warn("This JAR is a RustyConnector Smart provider. Move it from plugins/ to rc-modules/ at the proxy root and restart Velocity.");
    }
}
