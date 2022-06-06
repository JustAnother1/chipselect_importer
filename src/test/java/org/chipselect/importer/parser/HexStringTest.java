package org.chipselect.importer.parser;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

public class HexStringTest {

    @Test
    public void testHexString()
    {
        HexString cut = new HexString("0x123");
        assertNotNull(cut);
    }

    @Test
    public void testNull()
    {
        HexString cut = new HexString(null);
        assertNotNull(cut);
        assertEquals(null, cut.toString());
    }

    @Test
    public void testInvalid()
    {
        HexString cut = new HexString("Not a Number");
        assertNotNull(cut);
        assertEquals(null, cut.toString());
    }

    @Test
    public void testInvalidHex()
    {
        HexString cut = new HexString("0x");
        assertNotNull(cut);
        assertEquals(null, cut.toString());
    }

    @Test
    public void testLong()
    {
        HexString cut = new HexString(255);
        assertNotNull(cut);
        assertEquals("0xFF", cut.toString());
    }

    @Test
    public void testLongNull()
    {
        HexString cut = new HexString(0);
        assertNotNull(cut);
        assertEquals("0x0", cut.toString());
    }

    @Test
    public void testLongBig()
    {
        HexString cut = new HexString(305419896);
        assertNotNull(cut);
        assertEquals("0x12345678", cut.toString());
    }

    @Test
    public void testDescimal()
    {
        HexString cut = new HexString("255");
        assertNotNull(cut);
        assertEquals("0xFF", cut.toString());
    }

    @Test
    public void testDescimalWhitespace()
    {
        HexString cut = new HexString(" 255   ");
        assertNotNull(cut);
        assertEquals("0xFF", cut.toString());
    }

    @Test
    public void testDescimalRejectNegatives()
    {
        HexString cut = new HexString("-255");
        assertNotNull(cut);
        assertEquals(null, cut.toString());
    }

    @Test
    public void testHex()
    {
        HexString cut = new HexString("0XFF");
        assertNotNull(cut);
        assertEquals("0xFF", cut.toString());
    }

    @Test
    public void testSmallHex()
    {
        HexString cut = new HexString("0x123");
        assertNotNull(cut);
        assertEquals("0x123", cut.toString());
    }

    @Test
    public void testLongHex()
    {
        HexString cut = new HexString("0xfFff334455667788991122");
        assertNotNull(cut);
        assertEquals("0xFFFF334455667788991122", cut.toString());
    }

    @Test
    public void testNoPrefix()
    {
        HexString cut = new HexString("Dead123");
        assertNotNull(cut);
        assertEquals("0xDEAD123", cut.toString());
    }

    @Test
    public void testEqualsDescimal()
    {
        HexString cut = new HexString("255");
        assertNotNull(cut);
        assertTrue(cut.equals(255));
    }

    @Test
    public void testEqualsDescimalLong()
    {
        HexString cut = new HexString("73588229205");
        assertNotNull(cut);
        assertTrue(cut.equals(73588229205L));
    }

    @Test
    public void testEqualsHex()
    {
        HexString cut = new HexString("0xff");
        assertNotNull(cut);
        assertTrue(cut.equals(255));
    }

    @Test
    public void testEqualsHexLong()
    {
        HexString cut = new HexString("0x1122334455");
        assertNotNull(cut);
        assertTrue(cut.equals(73588229205L));
    }

    @Test
    public void testEqualsHexString()
    {
        HexString cut = new HexString("0xff");
        HexString other = new HexString("0xff");
        assertTrue(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringDecimal()
    {
        HexString cut = new HexString("0xff");
        HexString other = new HexString("255");
        assertTrue(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringNotEqual()
    {
        HexString cut = new HexString("0xff");
        HexString other = new HexString("254");
        assertFalse(cut.equals(other));
    }

    @Test
    public void testEqualsStringHexString()
    {
        HexString cut = new HexString("0xff");
        assertTrue(cut.equals("0XFF"));
    }

    @Test
    public void testEqualsHexStringHexString()
    {
        HexString cut = new HexString("0xff");
        HexString other = new HexString("0xff");
        assertTrue(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringHexString_dec()
    {
        HexString cut = new HexString("0xff");
        HexString other = new HexString("255");
        assertTrue(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringHexString_zero()
    {
        HexString cut = new HexString("0x0");
        HexString other = new HexString("0");
        assertTrue(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringHexString_one()
    {
        HexString cut = new HexString("0x0");
        HexString other = new HexString("1");
        assertFalse(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringHexString_null()
    {
        HexString cut = new HexString("0x0");
        HexString other = null;
        assertFalse(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringHexString_null_val()
    {
        HexString cut = new HexString(null);
        HexString other = new HexString("0");
        assertFalse(cut.equals(other));
    }

    @Test
    public void testEqualsStringHexStringDecimal()
    {
        HexString cut = new HexString("0xff");
        assertTrue(cut.equals("255"));
    }

    @Test
    public void testEqualsHexStringLeadingZeros()
    {
        HexString cut = new HexString("0x0000ff");
        HexString other = new HexString("0xff");
        assertTrue(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringLeadingZerosOther()
    {
        HexString cut = new HexString("0ff");
        HexString other = new HexString("0x0000ff");
        assertTrue(cut.equals(other));
    }

    @Test
    public void testEqualsHexStringLeadingZerosOtherString()
    {
        HexString cut = new HexString("0ff");
        assertTrue(cut.equals("0x000000ff"));
    }

    @Test
    public void testAdd()
    {
        HexString cut = new HexString("230");
        HexString out = cut.add(520);
        // 750 = 0x2EE
        assertEquals("0x2EE", out.toString());
    }

    @Test
    public void testAddLongLonger()
    {
        HexString cut = new HexString("0xF");
        HexString out = cut.add(255);
        assertEquals("0x10E", out.toString());
    }

    @Test
    public void testAddValLonger()
    {
        HexString cut = new HexString("0xFFF");
        HexString out = cut.add(5);
        assertEquals("0x1004", out.toString());
    }

    @Test
    public void testAdd_singleDigitDecimal()
    {
        HexString cut = new HexString("2");
        HexString out = cut.add(5);
        HexString should = new HexString("7");
        assertTrue(out.equals(should));
    }

    @Test
    public void testHashMap()
    {
        HexString zero = new HexString("0x0");
        HexString one = new HexString("0x1");
        HexString two = new HexString("2");
        HexString four = new HexString("4");
        HashMap<HexString, Integer> map = new HashMap<HexString, Integer>();
        map.put(zero, 0);
        map.put(one, 1);
        map.put(two, 2);
        map.put(four, 4);
        HexString three = new HexString("3");
        assertFalse(map.containsKey(three));
        assertTrue(map.containsKey(zero));
        HexString oneMoreThanOne = new HexString("2");
        assertTrue(oneMoreThanOne.equals(two));
        assertTrue(map.containsKey(oneMoreThanOne));
        assertTrue(map.containsKey(four));
    }
}
