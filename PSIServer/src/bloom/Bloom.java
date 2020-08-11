package bloom;

/**
 * @author: kascas
 * @date: 2020/8/8-22:34
 * @description:
 */
public class Bloom {

    private int entries, bits, bytes, hashes, ready;
    private double error, bpe;
    private byte[] bf;

    public boolean bloom_init(int entries, double error) {
        ready = 1;
        if (entries < 1000 || error == 0)
            return false;
        this.entries = entries;
        this.error = error;
        double num = Math.log(error);
        double denom = 0.480453013918201;
        bpe = -(num / denom);
        bits = (int) ((double) entries * bpe);
        if (bits % 8 == 0)
            bytes = bits / 8;
        else
            bytes = (bits / 8) + 1;
        hashes = (int) Math.ceil(0.693147180559945 * bpe);
        bf = new byte[bytes];
        return true;
    }

    private boolean bloom_check_add(String buffer, int add) {
        if (ready == 0) {
            System.out.println("bloom has not been initialized");
            return false;
        }
        byte[] buf = buffer.getBytes();
        long x, hits = 0;
        long a = Murmurhash.hash32(buf, buf.length, 0x9747b28c) & 0xffffffffL;
        long b = Murmurhash.hash32(buf, buf.length, (int) a) & 0xffffffffL;
        for (int i = 0; i < hashes; i++) {
            x = (a + i * b) % bits;
            if (test_bit_set_bit(bf, x, add) == 1)
                hits++;
            else if (add == 0)
                return false;
        }
        return (hits == hashes);
    }

    private boolean bloom_check_add(byte[] buf, int add) {
        if (ready == 0) {
            System.out.println("bloom has not been initialized");
            return false;
        }
        //byte[] buf = buffer.getBytes();
        long x, hits = 0;
        long a = Murmurhash.hash32(buf, buf.length, 0x9747b28c) & 0xffffffffL;
        long b = Murmurhash.hash32(buf, buf.length, (int) a) & 0xffffffffL;
        for (int i = 0; i < hashes; i++) {
            x = (a + i * b) % bits;
            if (test_bit_set_bit(bf, x, add) == 1)
                hits++;
            else if (add == 0)
                return false;
        }
        return (hits == hashes);
    }

    public boolean bloom_check(String buffer) {
        return bloom_check_add(buffer, 0);
    }
    public boolean bloom_check(byte[] buffer) {
        return bloom_check_add(buffer, 0);
    }

    public void bloom_add(String buffer) {
        boolean addResult = bloom_check_add(buffer, 1);
    }

    public void bloom_add(byte[] buffer) {
        boolean addResult = bloom_check_add(buffer, 1);
    }

    public void bloom_print() {
        System.out.println("------BLOOM------");
        System.out.print("-->entries: ");
        System.out.println(entries);
        System.out.print("-->error: ");
        System.out.println(error);
        System.out.print("-->bits: ");
        System.out.println(bits);
        System.out.print("-->bpe: ");
        System.out.println(bpe);
        System.out.print("-->bytes: ");
        System.out.println(bytes);
        System.out.print("-->hashes: ");
        System.out.println(hashes);
        System.out.println("-----------------");
    }

    private int test_bit_set_bit(byte[] buf, long x, int set_bit) {
        int b = (int) (x >> 3);
        long c = buf[b];
        long mask = 1 << (x % 8);
        if ((c & mask) != 0)
            return 1;
        else if (set_bit == 1)
            buf[b] = (byte) (c | mask);
        return 0;
    }
}