package ma.daret.common.pii;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Logback converter that masks obvious PII (emails, phone-like digit runs) from log
 * messages (spec §12) — a defence-in-depth backstop so PII never lands in log files
 * even if a call site forgets. Wired as {@code %maskedMsg} in {@code logback-spring.xml}.
 */
public class PiiMaskingConverter extends MessageConverter {

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE =
            Pattern.compile("\\+?\\d[\\d\\s-]{7,}\\d");

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null || message.isEmpty()) {
            return message;
        }
        message = EMAIL.matcher(message).replaceAll("***@***");
        message = PHONE.matcher(message).replaceAll("***");
        return message;
    }
}
