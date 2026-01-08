import java.util.LinkedList;
import java.util.ArrayList;
public class HashTable <V>{
    private static final int DEFAULT_CAPACITY = 11;
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;
    private LinkedList<Entry>[] table;
    public int size;
    private int capacity;
    private class Entry{
        String key;
        V value;
        Entry(String key, V value){
            this.key=key;
            this.value=value;
        }
    }
    public HashTable() {
        this.capacity=DEFAULT_CAPACITY;
        this.size=0;
        this.table=new LinkedList[this.capacity];
    }
    public HashTable(int capacity) {
        this.capacity=capacity;
        this.size=0;
        this.table=new LinkedList[this.capacity];
    }
    private int hash(String key){
        return Math.abs(key.hashCode())%capacity;
    }
    public void put(String key, V value){
        int index = hash(key);
        if (table[index]==null){
            table[index]=new LinkedList<>();
        }
        //update value if key already exists
        for (Entry entry : table[index]){
            if (entry.key.equals(key)){
                entry.value=value;
                return;
            }
        }
        table[index].add(new Entry(key, value));
        size++;
        if ((double)size/capacity>LOAD_FACTOR_THRESHOLD){
            resize();
        }
    }
    public V get(String key){
        int index = hash(key);
        if (table[index]==null){
            return null;
        }
        for (Entry entry : table[index]){
            if (entry.key.equals(key)){
                return entry.value;
            }
        }
        return null;
    }
    //doubles the size oof the table and rehashes all entries
    private void resize(){
        capacity=capacity*2;
        LinkedList<Entry>[] oldTable = table;
        table=new LinkedList[capacity];
        size=0;
        for (LinkedList<Entry> bucket : oldTable){
            if (bucket!=null){
                for (Entry entry : bucket){
                    put(entry.key, entry.value);
                }
            }
        }
    }
    public void remove(String key) {
        int index = hash(key);
        if (table[index] == null) {
            return;
        }
        LinkedList<Entry> bucket = table[index];
        for (int i = 0; i < bucket.size(); i++) {
            if (bucket.get(i).key.equals(key)) {
                bucket.remove(i);
                size--;
                return;
            }
        }
    }
    public ArrayList<V> values(){
        ArrayList<V> values = new ArrayList<>();
        for (LinkedList<Entry> bucket : table){
            if (bucket!=null){
                for (Entry entry : bucket){
                    values.add(entry.value);
                }
            }
        }
        return values;
    }
    public boolean containsKey(String key) {
        int index = hash(key);
        if (table[index] == null) {
            return false;
        }
        for (Entry entry : table[index]) {
            if (entry.key.equals(key)) {
                return true;
            }
        }
        return false;
    }
}
