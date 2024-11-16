package application;

import java.util.*;

class QueueManager {
    private List<Song> queue;
    private int currentIndex;

    public QueueManager() {
        this.queue = new ArrayList<>();
        this.currentIndex = -1;
    }

    public void setQueue(List<Song> songs) {
        this.queue = new ArrayList<>(songs);
        this.currentIndex = -1;
    }

    public void addToQueue(Song song) {
        queue.add(song);
    }

    public void removeFromQueue(int index) {
        if (index >= 0 && index < queue.size()) {
            queue.remove(index);
            if (index < currentIndex) currentIndex--;
        }
    }

    public Song getNextSong() {
        if (queue.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % queue.size();
        return queue.get(currentIndex);
    }

    public Song getPreviousSong() {
        if (queue.isEmpty()) return null;
        currentIndex = (currentIndex - 1 + queue.size()) % queue.size();
        return queue.get(currentIndex);
    }

    public List<Song> getQueue() {
        return new ArrayList<>(queue);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}