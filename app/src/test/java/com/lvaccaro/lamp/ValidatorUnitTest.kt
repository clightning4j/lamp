package com.lvaccaro.lamp

import com.lvaccaro.lamp.util.Validator
import org.junit.Test

import org.junit.Assert.*

/**
 * Resources to find the address used inside this test are here
 * https://en.bitcoin.it/wiki/List_of_address_prefixes
 *
 * @author https://github.com/vincenzopalazzo
 */
class ValidatorUnitTest {

    @Test
    fun validateBitcoinURL_testOne() {
        val resultParsing = Validator.doParseBitcoinURL("")
        assertEquals(0, resultParsing.size)
    }

    @Test
    fun validateBitcoinURL_testTwo() {
        val resultParsing = Validator.doParseBitcoinURL("bitcoin:2NFmhFFEbR2ruAboRZ8gxCeDez81c3ByZeV?amount=102.00000000")
        assertEquals(2, resultParsing.size)
        assertEquals("2NFmhFFEbR2ruAboRZ8gxCeDez81c3ByZeV", resultParsing[0])
        assertEquals("102.00000000", resultParsing[1])
    }

    @Test
    fun validateBitcoinURL_testThree() {
        val resultParsing = Validator.doParseBitcoinURL("2NFmhFFEbR2ruAboRZ8gxCeDez81c3ByZeV")
        assertEquals(0, resultParsing.size)
    }

    @Test //P2PKH
    fun validateBitcoinAddress_testOne() {
        val resultBitcoin = Validator.isBitcoinAddress("17VZNX1SN5NtKa8UQFxwQbFeFc3iqRYhem")
        val resultTestnet = Validator.isBitcoinAddress("mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn")
        val resultRegtest = Validator.isBitcoinAddress("mjkAUDGxzLiHsUNSmYZXYzbahkrxioELgR")
        assertEquals(true, resultBitcoin)
        assertEquals(true, resultTestnet)
        assertEquals(true, resultRegtest)
    }

    @Test // P2SH
    fun validateBitcoinAddress_testTwo() {
        val resultBitcoin = Validator.isBitcoinAddress("3EktnHQD7RiAE6uzMj2ZifT9YgRrkSgzQX")
        val resultTestnet = Validator.isBitcoinAddress("2N8ozcZ17d8ikcqzBRpjEh2Z3HTZ5F1myv4")
        val resultRegtest = Validator.isBitcoinAddress("2N1VQ42X2Fn6qq2jqK9uV95dgsh97UapKUG")
        assertEquals(true, resultBitcoin)
        assertEquals(true, resultTestnet)
        assertEquals(true, resultRegtest)
    }


    @Test
    fun validateBitcoinAddress_testThree() {
        val resultBitcoin = Validator.isBitcoinAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")
        val resultTestnet = Validator.isBitcoinAddress("tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx")
        val resultRegtest = Validator.isBitcoinAddress("bcrt1q7ezrtfqx2w4wg6vmlsk9uaxqeuxgpewdn09zc8")
        assertEquals(true, resultBitcoin)
        assertEquals(true, resultTestnet)
        assertEquals(true, resultRegtest)
    }

    @Test
    fun validateBitcoinAddress_testFour() {
        val resultBitcoin = Validator.isBitcoinAddress("bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3")
        val resultTestnet = Validator.isBitcoinAddress("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7")
        assertEquals(true, resultBitcoin)
        assertEquals(true, resultTestnet)
    }

    @Test
    fun validateBitcoinAddress_testFive() {
        val result = Validator.isBitcoinAddress("1Alibaba")
        assertEquals(false, result)
    }


}
