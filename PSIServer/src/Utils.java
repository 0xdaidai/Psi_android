import bloom.Bloom;
import com.github.mgunlogson.cuckoofilter4j.CuckooFilter;
import com.google.gson.Gson;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Utils {

    private static BufferedOutputStream out = null;
    private static int i = 0;
    public static void writeLineToFile(File file, byte[] line, int begin, int end) {

        if (begin == 0) {
            try {
                out = new BufferedOutputStream(new FileOutputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        try {
            i++;
            out.write(line);
            System.out.println("lineDB i: "+i);
            System.out.println("lineDB: "+ bytesToBigInteger(line,0,line.length).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (begin == end - 1) {
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendBytes(Socket socket, byte[] bytes) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] receiveBytes(Socket socket) {
        byte[] bytes = null;
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            bytes = (byte[])in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    public static void send2DBytes(Socket socket, byte[][] bytes) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendInteger(DataOutputStream d_out, long integer) {
        try {
            d_out.writeLong(integer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendString(Socket socket, String s) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter
                    (new OutputStreamWriter(socket.getOutputStream())), true);
            out.println(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String receiveString(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void sendFile(DataOutputStream d_out, File file) {
        try {
            FileInputStream f_in = new FileInputStream(file);

            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = (f_in.read(buffer))) > 0) {
                d_out.write(buffer, 0, read);
            }
            f_in.close();
            d_out.flush();
            //		dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BigInteger stringToBigInteger(String s) {
        byte[] input = null;
        try {
            input = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bytesToBigInteger(input, 0, input.length);
    }

    public static BigInteger bytesToBigInteger(byte[] in, int inOff, int inLen) {
        byte[]  block;

        if (inOff != 0 || inLen != in.length) {
            block = new byte[inLen];

            System.arraycopy(in, inOff, block, 0, inLen);
        } else {
            block = in;
        }

        BigInteger res = new BigInteger(1, block);

        return res;
    }

    public static byte[] bigIntegerToBytes(BigInteger input, boolean forEncryption) {
        byte[] output = input.toByteArray();

	/*	if (forEncryption) {
			if (output[0] == 0 && output.length > getOutputBlockSize(forEncryption)) {       // have ended up with an extra zero byte, copy down.
				byte[]  tmp = new byte[output.length - 1];
				System.arraycopy(output, 1, tmp, 0, tmp.length);
				return tmp;
			}

			if (output.length < getOutputBlockSize(forEncryption)) {    // have ended up with less bytes than normal, lengthen
				byte[]  tmp = new byte[getOutputBlockSize(forEncryption)];
				System.arraycopy(output, 0, tmp, tmp.length - output.length, output.length);
				return tmp;
			}
		} else {*/
        if (output[0] == 0) {        // have ended up with an extra zero byte, copy down.
            byte[]  tmp = new byte[output.length - 1];
            System.arraycopy(output, 1, tmp, 0, tmp.length);
            return tmp;
        }
//		}

        return output;
    }

    static byte[] sha1(String input, int length) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());

        return Arrays.copyOfRange(result, 0, length/8); //128 bits
    }

    static String bytesToBinaryString(byte[] input) {
        StringBuilder output = new StringBuilder("");
        for (int i = 0; i < input.length; i ++) {
            output = output.append(String.format("%8s", Integer.toBinaryString(input[i] & 0xFF)).replace(' ', '0'));
        }
        return output.toString();
    }

    static byte[] padBytes(byte[] input, int size) {
        byte[] output = new byte[size];
        int start = size - input.length;
        System.arraycopy(input, 0, output, start, input.length);
        return output;
    }

    public static void cuckooWriter(CuckooFilter<byte[]> filter, File file) {
        ObjectOutputStream out = null;
        FileOutputStream f_out = null;
        try {
            f_out = new FileOutputStream(file);
            out = new ObjectOutputStream(f_out);
            out.writeObject(filter);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static CuckooFilter cuckooReader(File file) {
        FileInputStream fileIn = null;
        ObjectInputStream in = null;
        CuckooFilter filter = null;
        try {
            fileIn = new FileInputStream(file);
            in = new ObjectInputStream(fileIn);
            filter =  (CuckooFilter<byte[]>)in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                if(in != null)
                    in.close();
                if(fileIn != null)
                    fileIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filter;
    }

    /*
    // bloomIO
    public static void bloomWriter(Bloom bloom, String file) {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(bloom);

        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            bw.write(jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bloom bloomReader(String file) {
        Gson gson = new Gson();
        String line = null, jsonStr;
        StringBuilder jsonStrBuilder = new StringBuilder();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                jsonStrBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
                if (fr != null)
                    fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        jsonStr = jsonStrBuilder.toString();

        return gson.fromJson(jsonStr, Bloom.class);
    }

     */

/*
	public static int getOutputBlockSize(boolean forEncryption) {
        int bitSize = ((RSAKeyParameters)keyPair.getPublic()).getModulus().bitLength();

        if (forEncryption) {
            return (bitSize + 7) / 8;
        }
        else {
            return (bitSize + 7) / 8 - 1;
        }
    }*/
}
