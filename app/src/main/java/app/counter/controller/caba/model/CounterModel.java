package app.counter.controller.caba.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * CounterModel - represents a single counter card with all features from index.html
 */
public class CounterModel implements Serializable {
    private String id;
    private String label;
    private int value;
    private int step;
    private int target;
    private String colorTag; // red, orange, yellow, green, blue, purple, pink, gray
    private String category;
    private boolean locked;
    private String notes;
    
    // Timer/Stopwatch addon
    private String addonType; // "none", "stopwatch", "timer"
    private long swElapsedMs;
    private long swLastTs;
    private boolean swRunning;
    private int tInitSec;
    private int tRemainSec;
    private long tLastTs;
    private boolean tRunning;
    
    // History tracking
    private long createdAt;
    private long lastModified;
    private int totalIncrements;
    private int totalDecrements;
    private int maxValue;
    private int minValue;
    private boolean targetNotified;
    
    public CounterModel() {
        this.id = UUID.randomUUID().toString();
        this.label = "Counter";
        this.value = 0;
        this.step = 1;
        this.target = 0;
        this.colorTag = "gray";
        this.category = "";
        this.locked = false;
        this.notes = "";
        this.addonType = "none";
        this.swElapsedMs = 0;
        this.swLastTs = 0;
        this.swRunning = false;
        this.tInitSec = 60;
        this.tRemainSec = 60;
        this.tLastTs = 0;
        this.tRunning = false;
        this.createdAt = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
        this.totalIncrements = 0;
        this.totalDecrements = 0;
        this.maxValue = 0;
        this.minValue = 0;
        this.targetNotified = false;
    }
    
    public CounterModel(String label) {
        this();
        this.label = label;
    }
    
    public CounterModel(String label, int value, int step, int target) {
        this(label);
        this.value = value;
        this.step = step;
        this.target = target;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { 
        this.label = label; 
        this.lastModified = System.currentTimeMillis();
    }

    public int getValue() { return value; }
    public void setValue(int value) { 
        this.value = value;
        this.lastModified = System.currentTimeMillis();
        if (value > maxValue) maxValue = value;
        if (value < minValue) minValue = value;
    }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = Math.max(1, step); }

    public int getTarget() { return target; }
    public void setTarget(int target) { 
        this.target = Math.max(0, target); 
        this.targetNotified = false;
    }

    public String getColorTag() { return colorTag; }
    public void setColorTag(String colorTag) { this.colorTag = colorTag; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAddonType() { return addonType; }
    public void setAddonType(String addonType) { this.addonType = addonType; }

    public long getSwElapsedMs() { return swElapsedMs; }
    public void setSwElapsedMs(long swElapsedMs) { this.swElapsedMs = swElapsedMs; }

    public long getSwLastTs() { return swLastTs; }
    public void setSwLastTs(long swLastTs) { this.swLastTs = swLastTs; }

    public boolean isSwRunning() { return swRunning; }
    public void setSwRunning(boolean swRunning) { this.swRunning = swRunning; }

    public int getTInitSec() { return tInitSec; }
    public void setTInitSec(int tInitSec) { this.tInitSec = tInitSec; }

    public int getTRemainSec() { return tRemainSec; }
    public void setTRemainSec(int tRemainSec) { this.tRemainSec = tRemainSec; }

    public long getTLastTs() { return tLastTs; }
    public void setTLastTs(long tLastTs) { this.tLastTs = tLastTs; }

    public boolean isTRunning() { return tRunning; }
    public void setTRunning(boolean tRunning) { this.tRunning = tRunning; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public int getTotalIncrements() { return totalIncrements; }
    public void setTotalIncrements(int totalIncrements) { this.totalIncrements = totalIncrements; }

    public int getTotalDecrements() { return totalDecrements; }
    public void setTotalDecrements(int totalDecrements) { this.totalDecrements = totalDecrements; }

    public int getMaxValue() { return maxValue; }
    public void setMaxValue(int maxValue) { this.maxValue = maxValue; }

    public int getMinValue() { return minValue; }
    public void setMinValue(int minValue) { this.minValue = minValue; }
    
    public boolean isTargetNotified() { return targetNotified; }
    public void setTargetNotified(boolean targetNotified) { this.targetNotified = targetNotified; }

    // Helper methods
    public boolean increment() {
        if (!locked) {
            value += step;
            totalIncrements++;
            lastModified = System.currentTimeMillis();
            if (value > maxValue) maxValue = value;
            return isTargetReached() && !targetNotified;
        }
        return false;
    }

    public void decrement() {
        if (!locked) {
            value -= step;
            totalDecrements++;
            lastModified = System.currentTimeMillis();
            if (value < minValue) minValue = value;
        }
    }

    public void reset() {
        if (!locked) {
            value = 0;
            targetNotified = false;
            lastModified = System.currentTimeMillis();
        }
    }

    public boolean isTargetReached() {
        return target > 0 && value >= target;
    }

    public int getProgressPercent() {
        if (target <= 0) return 0;
        return Math.min(100, (int)((value * 100.0) / target));
    }

    public String getFormattedStopwatch() {
        long ms = swElapsedMs;
        if (swRunning) {
            ms += System.currentTimeMillis() - swLastTs;
        }
        long secs = ms / 1000;
        long mins = secs / 60;
        long hours = mins / 60;
        return String.format("%02d:%02d:%02d", hours, mins % 60, secs % 60);
    }

    public String getFormattedTimer() {
        int secs = tRemainSec;
        int mins = secs / 60;
        int hours = mins / 60;
        return String.format("%02d:%02d:%02d", hours, mins % 60, secs % 60);
    }
    
    // Stopwatch controls
    public void startStopwatch() {
        if (!swRunning) {
            swRunning = true;
            swLastTs = System.currentTimeMillis();
        }
    }
    
    public void pauseStopwatch() {
        if (swRunning) {
            swElapsedMs += System.currentTimeMillis() - swLastTs;
            swRunning = false;
        }
    }
    
    public void resetStopwatch() {
        swRunning = false;
        swElapsedMs = 0;
        swLastTs = 0;
    }
    
    // Timer controls
    public void startTimer() {
        if (!tRunning && tRemainSec > 0) {
            tRunning = true;
            tLastTs = System.currentTimeMillis();
        }
    }
    
    public void pauseTimer() {
        if (tRunning) {
            long elapsed = (System.currentTimeMillis() - tLastTs) / 1000;
            tRemainSec = Math.max(0, tRemainSec - (int)elapsed);
            tRunning = false;
        }
    }
    
    public void resetTimer() {
        tRunning = false;
        tRemainSec = tInitSec;
        tLastTs = 0;
    }
    
    public boolean updateTimer() {
        if (tRunning) {
            long elapsed = (System.currentTimeMillis() - tLastTs) / 1000;
            if (elapsed > 0) {
                tRemainSec = Math.max(0, tRemainSec - (int)elapsed);
                tLastTs = System.currentTimeMillis();
                if (tRemainSec <= 0) {
                    tRunning = false;
                    return true; // Timer finished
                }
            }
        }
        return false;
    }
}
