import java.util.Optional;

public class FunctionalBubbleSort {
    static class Node {
        final int val;
        final Node next;

        Node(int val, Node next) {
            this.val = val;
            this.next = next;
        }
    }

    static class Result {
        final boolean swapped;
        final Node list;

        Result(boolean swapped, Node list) {
            this.swapped = swapped;
            this.list = list;
        }
    }

    public static Result inner(Node l) {
        if (l == null || l.next == null) {
            return new Result(false, l);
        }

        int x = l.val;
        Node z = l.next;
        int y = z.val;
        Node tl = z.next;

        if (x > y) {
            Result subResult = inner(new Node(x, tl));
            return new Result(true, new Node(y, subResult.list));
        } else {
            Result subResult = inner(z);
            return new Result(subResult.swapped, new Node(x, subResult.list));
        }
    }

    public static Node rec(Node l) {
        Result res = inner(l);
        if (res.swapped) {
            return rec(res.list);
        } else {
            return res.list;
        }
    }

    public static Node bubbleSort(Node l) {
        return rec(l);
    }

    public static Node generate(int n) {
        if (n > 0) {
            return new Node(n, generate(n - 1));
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        Node list = generate(10000);
        Node sorted = bubbleSort(list);
    }
}
