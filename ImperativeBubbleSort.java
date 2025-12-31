import java.util.ArrayList;
import java.util.List;

public class ImperativeBubbleSort {

    public static void bubbleSort(List<Integer> list) {
        int n = list.size();
        boolean swapped;

        do {
            swapped = false;
            for (int i = 0; i < n - 1; i++) {
                if (list.get(i) > list.get(i + 1)) {
                    int temp = list.get(i);
                    list.set(i, list.get(i + 1));
                    list.set(i + 1, temp);

                    swapped = true;
                }
            }
            n--;
        } while (swapped);
    }

    public static List<Integer> generate(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = n; i > 0; i--) {
            list.add(i);
        }
        return list;
    }

    public static void main(String[] args) {
        List<Integer> data = generate(10000);
        bubbleSort(data);
    }
}
