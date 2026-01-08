public class MinHeap <T extends Comparable<T>> {
    private T[] array;
    private int size;
    private static final int INITIAL_CAPACITY=11;
    public MinHeap(){
        array =(T[]) new Comparable[INITIAL_CAPACITY];
        size=0;
    }

    public void offer(T item){
        if (size == array.length-1){
            resize();
        }
        int hole=++size;
        for (array[0]=item; item.compareTo(array[hole/2])<0; hole/=2){
            array[hole]= array[hole/2];
        }
        array[hole]=item;
    }
    public T poll() {
        if (isEmpty()) return null;
        T minItem= array[1];
        array[1]= array[size--];
        percolateDown(1);
        return minItem;
    }
    private void percolateDown(int hole){
        int child;
        T tmp= array[hole];
        for (;hole*2<=size;hole=child){
            child=hole*2;
            if (child!=size && array[child+1].compareTo(array[child])<0) {
                child++;
            }
            if (array[child].compareTo(tmp)<0){
                array[hole]= array[child];
            } else {
                break;
            }
        }
        array[hole]=tmp;
    }
    public boolean isEmpty(){
        return size==0;
    }

    private void resize(){
        T[] newHeap= (T[]) new Comparable[array.length*2];
        System.arraycopy(array, 0, newHeap, 0, array.length);
        array =newHeap;
    }
}
