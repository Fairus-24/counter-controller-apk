package app.counter.controller.caba.model;

import java.io.Serializable;
import app.counter.controller.caba.model.CounterModel;
import java.util.ArrayList;
import java.util.List;

/**
 * App State - holds all counters and settings
 */
public class AppState implements Serializable {
    private String viewMode; // "row", "col", "grid"
    private boolean notesVisible;
    private String bigTitle;
    private String topText;
    private String theme; // "dark", "light"
    private boolean soundOn;
    private String fontMode; // "modern", "seven"
    private int alarmRingSec;
    private List<CounterModel> cards;
    
    // Sync state
    private boolean syncEnabled;
    private String syncSessionId;
    private long lastSyncTs;
    
    public AppState() {
        this.viewMode = "col";
        this.notesVisible = false;
        this.bigTitle = "";
        this.topText = "";
        this.theme = "dark";
        this.soundOn = true;
        this.fontMode = "modern";
        this.alarmRingSec = 15;
        this.cards = new ArrayList<>();
        this.syncEnabled = false;
        this.syncSessionId = "";
        this.lastSyncTs = 0;
    }

    // Getters and Setters
    public String getViewMode() { return viewMode; }
    public void setViewMode(String viewMode) { this.viewMode = viewMode; }

    public boolean isNotesVisible() { return notesVisible; }
    public void setNotesVisible(boolean notesVisible) { this.notesVisible = notesVisible; }

    public String getBigTitle() { return bigTitle; }
    public void setBigTitle(String bigTitle) { this.bigTitle = bigTitle; }

    public String getTopText() { return topText; }
    public void setTopText(String topText) { this.topText = topText; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public boolean isSoundOn() { return soundOn; }
    public void setSoundOn(boolean soundOn) { this.soundOn = soundOn; }

    public String getFontMode() { return fontMode; }
    public void setFontMode(String fontMode) { this.fontMode = fontMode; }

    public int getAlarmRingSec() { return alarmRingSec; }
    public void setAlarmRingSec(int alarmRingSec) { this.alarmRingSec = alarmRingSec; }

    public List<CounterModel> getCards() { return cards; }
    public void setCards(List<CounterModel> cards) { this.cards = cards; }

    public boolean isSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }

    public String getSyncSessionId() { return syncSessionId; }
    public void setSyncSessionId(String syncSessionId) { this.syncSessionId = syncSessionId; }

    public long getLastSyncTs() { return lastSyncTs; }
    public void setLastSyncTs(long lastSyncTs) { this.lastSyncTs = lastSyncTs; }
    
    // Helper methods
    public void addCard(CounterModel counter) {
        cards.add(counter);
    }
    
    public void removeCard(String id) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId().equals(id)) {
                cards.remove(i);
                break;
            }
        }
    }
    
    public CounterModel getCard(String id) {
        for (CounterModel c : cards) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }
    
    public void moveCard(int from, int to) {
        if (from >= 0 && from < cards.size() && to >= 0 && to < cards.size()) {
            CounterModel card = cards.remove(from);
            cards.add(to, card);
        }
    }
    
    public void resetAll() {
        for (CounterModel c : cards) {
            c.reset();
        }
    }
    
    public int getTotalValue() {
        int total = 0;
        for (CounterModel c : cards) {
            total += c.getValue();
        }
        return total;
    }
    
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        for (CounterModel c : cards) {
            String cat = c.getCategory();
            if (cat != null && !cat.isEmpty() && !categories.contains(cat)) {
                categories.add(cat);
            }
        }
        return categories;
    }
    // ...existing code...
}
