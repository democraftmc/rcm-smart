package fun.democraft.smart;

import com.google.inject.Inject;
import org.slf4j.Logger;

public final class MisplacedSmartPlugin {
    @Inject
    public MisplacedSmartPlugin(Logger logger) {
        logger.warn("This JAR is the RustyConnector Smart module. Move it from plugins/ to rc-modules/ at the proxy root and restart Velocity.");
    }
}
