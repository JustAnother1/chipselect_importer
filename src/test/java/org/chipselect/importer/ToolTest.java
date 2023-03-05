package org.chipselect.importer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ToolTest
{

	@Test
	public void testCleanupString()
	{
		String in = "  1 > 0  ";
		String out = Tool.cleanupString(in);
		assertEquals("1 &gt; 0", out);
	}

	@Test
	public void testCleanupString_fix()
	{
		String in = "  1 &gt 0  ";
		String out = Tool.cleanupString(in);
		assertEquals("1 &gt; 0", out);
	}

	@Test
	public void testCleanupString_newline_a()
	{
		String in = "A\nB";
		String out = Tool.cleanupString(in);
		assertEquals("A\\nB", out);
	}

	@Test
	public void testCleanupString_newline_b()
	{
		String in = "A\\nB";
		String out = Tool.cleanupString(in);
		assertEquals("A\\nB", out);
	}

    @Test
    public void testCleanupString_newline_c()
    {
        String in = "A\\nB\nC";
        String out = Tool.cleanupString(in);
        assertEquals("A\\nB\\nC", out);
    }

	@Test
	public void testCleanupString_newline()
	{
		String in = "one\\ntwo\nthree";
		String out = Tool.cleanupString(in);
		assertEquals("one\\ntwo\\nthree", out);
	}

	@Test
	public void testCleanupString_and()
	{
		String in = "GPIO_HI_OE & 0x1";
		String out = Tool.cleanupString(in);
		assertEquals("GPIO_HI_OE &amp; 0x1", out);
	}

	@Test
	public void testCleanupString_and_no_space()
	{
		String in = "Perform an atomic bit-clear on GPIO_HI_OE, i.e. `GPIO_HI_OE &= ~wdata`";
		String out = Tool.cleanupString(in);
		assertEquals("Perform an atomic bit-clear on GPIO_HI_OE, i.e. `GPIO_HI_OE &amp;= ~wdata`", out);
	}

}
