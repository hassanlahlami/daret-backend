package ma.daret.identity.user;

import java.util.UUID;

/** Reserved system accounts seeded by migration. */
public final class SystemUsers {

    private SystemUsers() {
    }

    /**
     * The platform itself, used only to occupy gap-filled slots (spec §5.4). This is
     * the sole point where platform capital touches a circle; its exposure is capped.
     */
    public static final UUID PLATFORM_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
}
