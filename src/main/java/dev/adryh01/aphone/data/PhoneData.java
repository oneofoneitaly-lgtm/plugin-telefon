package dev.adryh01.aphone.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Everything stored for one player's phone. */
public final class PhoneData {

    public record Sms(String fromNumber, String fromName, String text, long time) {}

    private final UUID uuid;
    private String name;
    private String number;
    private final Set<String> contacts = new LinkedHashSet<>();
    private final List<Sms> inbox = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private boolean silent;
    private boolean flashlight;
    private Ringtone ringtone = Ringtone.CLASSIC;
    private boolean receivedPhone;
    private int unread;
    private int missedCalls;
    private long lastEmergency;

    public PhoneData(UUID uuid, String name, String number) {
        this.uuid = uuid;
        this.name = name;
        this.number = number;
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public void name(String name) { this.name = name; }
    public String number() { return number; }
    public Set<String> contacts() { return contacts; }
    public List<Sms> inbox() { return inbox; }
    public List<String> notes() { return notes; }
    public boolean silent() { return silent; }
    public void silent(boolean v) { silent = v; }
    public boolean flashlight() { return flashlight; }
    public void flashlight(boolean v) { flashlight = v; }
    public Ringtone ringtone() { return ringtone; }
    public void ringtone(Ringtone r) { ringtone = r; }
    public boolean receivedPhone() { return receivedPhone; }
    public void receivedPhone(boolean v) { receivedPhone = v; }
    public int unread() { return unread; }
    public void unread(int v) { unread = Math.max(0, v); }
    public int missedCalls() { return missedCalls; }
    public void missedCalls(int v) { missedCalls = Math.max(0, v); }
    public long lastEmergency() { return lastEmergency; }
    public void lastEmergency(long v) { lastEmergency = v; }
}
