package com.lvaccaro.lamp

import com.lvaccaro.lamp.util.LampKeys
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
        assertTrue(resultParsing.isEmpty())
    }

    @Test
    fun validateBitcoinURL_testTwo() {
        val resultParsingOne =
            Validator.doParseBitcoinURL("bitcoin:2NFmhFFEbR2ruAboRZ8gxCeDez81c3ByZeV?amount=102.00000000")
        assertEquals(2, resultParsingOne.size)
        assertEquals("2NFmhFFEbR2ruAboRZ8gxCeDez81c3ByZeV", resultParsingOne[LampKeys.ADDRESS_KEY])
        assertEquals("102.00000000", resultParsingOne[LampKeys.AMOUNT_KEY])

        val resultParsingTwo =
            Validator.doParseBitcoinURL("bitcoin:175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W")
        assertEquals(1, resultParsingTwo.size)
        assertEquals(
            "175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W",
            resultParsingTwo.get(LampKeys.ADDRESS_KEY)
        )

        val resultParsingThree =
            Validator.doParseBitcoinURL("bitcoin:175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W?label=Luke-Jr")
        assertEquals(2, resultParsingThree.size)
        assertEquals(
            "175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W",
            resultParsingThree.get(LampKeys.ADDRESS_KEY)
        )
        assertEquals("Luke-Jr", resultParsingThree[LampKeys.LABEL_KEY])

        val resultParsingFour =
            Validator.doParseBitcoinURL("bitcoin:175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W?amount=20.3&label=Luke-Jr")
        assertEquals(3, resultParsingFour.size)
        assertEquals(
            "175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W",
            resultParsingFour.get(LampKeys.ADDRESS_KEY)
        )
        assertEquals("20.3", resultParsingFour[LampKeys.AMOUNT_KEY])
        assertEquals("Luke-Jr", resultParsingFour[LampKeys.LABEL_KEY])

        val resultParsingFive =
            Validator.doParseBitcoinURL("bitcoin:175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W?amount=50&label=Luke-Jr&message=Donation%20for%20project%20xyz\n")
        assertEquals(4, resultParsingFive.size)
        assertEquals(
            "175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W",
            resultParsingFive.get(LampKeys.ADDRESS_KEY)
        )
        assertEquals("50", resultParsingFive[LampKeys.AMOUNT_KEY])
        assertEquals("Luke-Jr", resultParsingFive[LampKeys.LABEL_KEY])
        assertEquals("Donation for project xyz", resultParsingFive[LampKeys.MESSAGE_KEY]?.trim())
    }

    @Test
    fun validateBitcoinURL_testThree() {
        val resultParsing = Validator.doParseBitcoinURL("2NFmhFFEbR2ruAboRZ8gxCeDez81c3ByZeV")
        assertTrue(resultParsing.isEmpty())
    }

    @Test
    fun validateBitcoinURI_testFour(){
        val uri = "bitcoin:175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W?req-somethingyoudontunderstand=50&req-somethingelseyoudontget=999"
        val resultParsing = Validator.doParseBitcoinURL(uri)
        assertEquals("175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W", resultParsing.get(LampKeys.ADDRESS_KEY))
        assertEquals("50", resultParsing.get("req-somethingyoudontunderstand"))
        assertEquals("999", resultParsing.get("req-somethingelseyoudontget"))
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
        val resultRegtestTwo = Validator.isBitcoinAddress("2N6qSSMGjWhwynjxn7WC27j2tfWZFrUeRPW")
        assertEquals(true, resultBitcoin)
        assertEquals(true, resultTestnet)
        assertEquals(true, resultRegtest)
        assertEquals(true, resultRegtestTwo)
    }


    @Test
    fun validateBitcoinAddress_testThree() {
        val resultBitcoin = Validator.isBitcoinAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")
        val resultTestnet = Validator.isBitcoinAddress("tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx")
        val resultRegtest =
            Validator.isBitcoinAddress("bcrt1q7ezrtfqx2w4wg6vmlsk9uaxqeuxgpewdn09zc8")
        assertEquals(true, resultBitcoin)
        assertEquals(true, resultTestnet)
        assertEquals(true, resultRegtest)
    }

    @Test
    fun validateBitcoinAddress_testFour() {
        val resultBitcoin =
            Validator.isBitcoinAddress("bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3")
        val resultTestnet =
            Validator.isBitcoinAddress("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7")
        assertEquals(true, resultBitcoin)
        assertEquals(true, resultTestnet)
    }

    @Test
    fun validateBitcoinAddress_testFive() {
        val result = Validator.isBitcoinAddress("1Alibaba")
        assertEquals(false, result)
    }

    @Test
    fun isABolt11_testOne() {
        val bolt11 =
            """
            lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd
            5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn
            449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w
            """.trimIndent().trim()
        val result = Validator.isBolt11(bolt11)
        assertTrue(result)
    }

    @Test
    fun isBolt11_testTwo() {
        val bolt11 =
            """
            lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpquwpc4curk03c9
            wlrswe78q4eyqc7d8d0xqzpuyk0sg5g70me25alkluzd2x62aysf2pyy8edtjeevuv4p2d5p76r4zkmneet7
            uvyakky2zr4cusd45tftc9c5fh0nnqpnl2jfll544esqchsrny
            """.trimIndent().trim()
        val result = Validator.isBolt11(bolt11)
        assertTrue(result)
    }

    @Test
    fun isBolt11_testThree() {
        val bolt11 = "bcrt1q7ezrtfqx2w4wg6vmlsk9uaxqeuxgpewdn09zc8"
        val result = Validator.isBolt11(bolt11)
        assertFalse(result)
    }

    @Test
    fun isBolt11_testFour() {
        val bolt11 =
            "0360dca2f35336d303643a7fb172ba6185b9086aa1fbd6063a1447050f2dda0f87@127.0.0.1:9735"
        val result = Validator.isBolt11(bolt11)
        assertFalse(result)
    }

    @Test
    fun isABolt11_testFive() {
        val bolt11 =
            """
            lnbcapvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd
            5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn
            449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w
            """.trimIndent().trim()
        val result = Validator.isBolt11(bolt11)
        assertFalse(result)
    }

    @Test
    fun isABolt11_testSix() {
        val bolt11 =
            """
            lnbcrt100n1p03chsnpp5n72j2e0ykwz7ue37e60qn623z9elq4fguv52ny7jjqmv990zajeqdql2pshjmt9de6zqamfw
            35zqurp09kk2eqxqyjw5qcqp2sp5h0renl745kd7c86z2g49lkuhua3j66an80szd58qdpeyzhdkrl3srzjqfnup6dhq3x
            ls5v5pw0ksgrx0gcerqpr2pd6y6ckhstql6qfncta6qq24qqqqqgqqqqqqqqpqqqqqzsqqc9qy9qsqfy8e5x6aya75n3ayd
            5e8rsvkmepgq9gwf8v9ram560y5vlrapk9pwjxcfewfpvvnw97v49xzlce6zqmc5znv0ke6e3un5lm4hk8uewqq5dug0s
            """.trimIndent().trim()
        val result = Validator.isBolt11(bolt11)
        assertFalse(result)
    }

    @Test
    fun isNodeID_testOne() {
        val nodeid =
            "0360dca2f35336d303643a7fb172ba6185b9086aa1fbd6063a1447050f2dda0f87@127.0.0.1:9735"
        val result = Validator.isLightningNodURI(nodeid)
        assertTrue(result)
    }

    @Test
    fun isNodeID_testTwo() {
        val nodeid = "0360dca2f35336d303643a7fb172ba6185b9086aa1fbd6063a1447050f2dda0f87"
        val result = Validator.isLightningNodURI(nodeid)
        assertTrue(result)
    }

    @Test
    fun isNodeID_testThree() {
        val nodeid = "bcrt1q7ezrtfqx2w4wg6vmlsk9uaxqeuxgpewdn09zc8"
        val result = Validator.isLightningNodURI(nodeid)
        assertFalse(result)
    }

    @Test
    fun isNodeID_testFour() {
        val nodeid =
            """
            lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpquwpc4curk03c9
            wlrswe78q4eyqc7d8d0xqzpuyk0sg5g70me25alkluzd2x62aysf2pyy8edtjeevuv4p2d5p76r4zkmneet7
            uvyakky2zr4cusd45tftc9c5fh0nnqpnl2jfll544esqchsrny
            """.trimIndent().trim()
        val result = Validator.isLightningNodURI(nodeid)
        assertFalse(result)
    }

    @Test
    fun isNodeID_testFive() {
        val nodeid =
            """
            0219f8900ee78a89f050c24d8b69492954f9fdbabed753710845eb75d3a75a5880@139.162.159.79:9735
            """.trimIndent().trim()
        val result = Validator.isLightningNodURI(nodeid)
        assertTrue(result)
    }

    @Test
    fun isCorrectValue_testOne() {
        val nodeid =
            """
            0219f8900ee78a89f050c24d8b69492954f9fdbabed753710845eb75d3a75a5880@139.162.159.79:9735
            """.trimIndent().trim()
        val bitcoinAddress = Validator.isBitcoinAddress(nodeid)
        val bitcoinURI = Validator.isBitcoinURL(nodeid)
        val bolt11 = Validator.isBolt11(nodeid)
        val nodeURL = Validator.isLightningNodURI(nodeid)
        assertFalse(bitcoinAddress)
        assertFalse(bitcoinURI)
        assertFalse(bolt11)
        assertTrue(nodeURL)
    }

    @Test
    fun isCorrectValue_testTwo() {
        val invoice =
            """
            lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpquwpc4curk03c9
            wlrswe78q4eyqc7d8d0xqzpuyk0sg5g70me25alkluzd2x62aysf2pyy8edtjeevuv4p2d5p76r4zkmneet7
            uvyakky2zr4cusd45tftc9c5fh0nnqpnl2jfll544esqchsrny
            """.trimIndent().trim()
        val bitcoinAddress = Validator.isBitcoinAddress(invoice)
        val bitcoinURI = Validator.isBitcoinURL(invoice)
        val bolt11 = Validator.isBolt11(invoice)
        val nodeURL = Validator.isLightningNodURI(invoice)
        assertFalse(bitcoinAddress)
        assertFalse(bitcoinURI)
        assertTrue(bolt11)
        assertFalse(nodeURL)
    }

    @Test
    fun isCorrectValue_testThree() {
        val adderss =
            """
            bcrt1q7ezrtfqx2w4wg6vmlsk9uaxqeuxgpewdn09zc8
            """.trimIndent().trim()
        val bitcoinAddress = Validator.isBitcoinAddress(adderss)
        val bitcoinURI = Validator.isBitcoinURL(adderss)
        val bolt11 = Validator.isBolt11(adderss)
        val nodeURL = Validator.isLightningNodURI(adderss)
        assertTrue(bitcoinAddress)
        assertFalse(bitcoinURI)
        assertFalse(bolt11)
        assertFalse(nodeURL)
    }
}
