package com.example.myapplication.util;

import java.math.BigInteger;
import java.util.Random;

/**
 * @author: kascas
 * @date: 2020/8/8-22:34
 * @description:
 */
public class Rand {

    public static BigInteger randInt(int bitLen) {
        return new BigInteger(bitLen, new Random());
    }

    public static BigInteger randPrime(int bitLen) {
        return BigInteger.probablePrime(bitLen, new Random());
    }

}
