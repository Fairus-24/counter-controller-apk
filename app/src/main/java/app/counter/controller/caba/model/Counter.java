package app.counter.controller.caba.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Model class untuk Counter
 */
public class Counter implements Serializable {
    private String id;
    private String title;
    private String note;
    private int count;
    private int step;
    private int target;
    private boolean goalReached;
    private long createdAt;
    private long updatedAt;

    public Counter() {
        this.id = UUID.randomUUID().toString();
        this.title = "";
        this.note = "";
        this.count = 0;
        this.step = 1;
        this.target = 0;
        this.goalReached = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Counter(String title) {
        this();
        this.title = title;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getNote() { return note; }
    public int getCount() { return count; }
    public int getStep() { return step; }
    public int getTarget() { return target; }
    public boolean isGoalReached() { return goalReached; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { 
        this.title = title; 
        this.updatedAt = System.currentTimeMillis();
    }
    public void setNote(String note) { 
        this.note = note; 
        this.updatedAt = System.currentTimeMillis();
    }
    public void setCount(int count) { 
        this.count = count; 
        this.updatedAt = System.currentTimeMillis();
        checkGoal();
    }
    public void setStep(int step) { 
        this.step = Math.max(1, step); 
        this.updatedAt = System.currentTimeMillis();
    }
    public void setTarget(int target) { 
        this.target = Math.max(0, target);
        this.updatedAt = System.currentTimeMillis();
        checkGoal();
    }
    public void setGoalReached(boolean goalReached) { this.goalReached = goalReached; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Actions
    public void increment() {
        this.count += step;
        this.updatedAt = System.currentTimeMillis();
        checkGoal();
    }

    public void decrement() {
        this.count -= step;
        this.updatedAt = System.currentTimeMillis();
    }

    public void reset() {
        this.count = 0;
        this.goalReached = false;
        this.updatedAt = System.currentTimeMillis();
    }

    private void checkGoal() {
        if (target > 0 && count >= target && !goalReached) {
            goalReached = true;
        }
    }

    public int getProgress() {
        if (target <= 0) return 0;
        return Math.min(100, (count * 100) / target);
    }
}
