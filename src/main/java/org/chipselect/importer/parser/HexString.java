package org.chipselect.importer.parser;

import java.math.BigInteger;

public class HexString
{
    private final String val;

    public HexString(long value)
    {
        val = longToString(value);
    }

    public HexString(String value)
    {
        if(null == value)
        {
            val = null;
        }
        else
        {
            boolean isHex = false;
            boolean isValid = true;
            boolean hasPrefix = false;
            value = value.trim();
            // is it hex?
            if(value.startsWith("0x"))
            {
                isHex = true;
                hasPrefix = true;
            }
            else if(value.startsWith("0X"))
            {
                isHex = true;
                hasPrefix = true;
            }
            for(int i = 0; i < value.length(); i++)
            {
                char c = value.charAt(i);
                switch(c)
                {
                case 'x':
                case 'X':
                case 'a':
                case 'A':
                case 'b':
                case 'B':
                case 'c':
                case 'C':
                case 'd':
                case 'D':
                case 'e':
                case 'E':
                case 'f':
                case 'F':
                    isHex = true;
                    break;

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    // OK
                    break;

                default:
                    isValid = false;
                    break;
                }
            }
            if(isValid)
            {
                if(isHex)
                {
                    // hex
                    val = hexToString(hasPrefix, value);
                }
                else
                {
                    // decimal
                    val = decimalToString(value);
                }
            }
            else
            {
                val = null;
            }
        }
    }

    private String hexToString(boolean hasPrefix, String value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("0x");
        int start = 0;
        if(hasPrefix)
        {
            start = 2;
        }
        boolean first = true;
        if(start < value.length())
        {
            for(int i = start; i < value.length(); i++)
            {
                char c = value.charAt(i);
                switch(c)
                {
                case 'a':
                case 'A':
                    first = false;
                    sb.append("A");
                    break;

                case 'b':
                case 'B':
                    first = false;
                    sb.append("B");
                    break;

                case 'c':
                case 'C':
                    first = false;
                    sb.append("C");
                    break;

                case 'd':
                case 'D':
                    first = false;
                    sb.append("D");
                    break;

                case 'e':
                case 'E':
                    first = false;
                    sb.append("E");
                    break;

                case 'f':
                case 'F':
                    first = false;
                    sb.append("F");
                    break;

                case '0':
                    if(true == first)
                    {
                        // skip leading zero
                    }
                    else
                    {
                        sb.append(c);
                    }
                    break;

                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    first = false;
                    sb.append(c);
                    break;
                }
            }
            if(true == first)
            {
                // String is 0x0 or 0x00, or 0x000,...
                return "0x0";
            }
            else
            {
                return sb.toString();
            }
        }
        else
        {
            return null;
        }
    }

    private String decimalToString(String value)
    {
        BigInteger bi = new BigInteger(value);
        StringBuilder sb = new StringBuilder();
        BigInteger sixteen = new BigInteger("16");
        while(bi.compareTo(sixteen) != -1) // // bigger than 16(=1) or 16(=0)
        {
            BigInteger i = bi.mod(sixteen);
            bi = bi.subtract(i);
            sb.append(intToChar(i.longValue()));
            bi = bi.divide(sixteen);
        }
        sb.append(intToChar(bi.longValue()));
        sb.reverse();
        return "0x" + sb.toString();
    }

    private String longToString(long value)
    {
        StringBuilder sb = new StringBuilder();
        while(value > 15)
        {
            long i = value%16;
            value = value - i;
            sb.append(intToChar(i));
            value = value /16;
        }
        sb.append(intToChar(value));
        sb.reverse();
        return "0x" + sb.toString();
    }

    private char intToChar(long value)
    {
        int i = (int)value;
        switch(i)
        {
        case  0: return '0';
        case  1: return '1';
        case  2: return '2';
        case  3: return '3';
        case  4: return '4';
        case  5: return '5';
        case  6: return '6';
        case  7: return '7';
        case  8: return '8';
        case  9: return '9';
        case 10: return 'A';
        case 11: return 'B';
        case 12: return 'C';
        case 13: return 'D';
        case 14: return 'E';
        case 15: return 'F';
        default: return 'N';
        }
    }

    public boolean equals(HexString other)
    {
        String otherVal = other.toString();
        return val.equals(otherVal);
    }

    public boolean equals(String other)
    {
        HexString o = new HexString(other);
        String oVal = o.toString();
        return val.equals(oVal);
    }

    public boolean equals(int other)
    {
        String otherVal = longToString(other);
        return val.equals(otherVal);
    }

    public boolean equals(long other)
    {
        String otherVal = longToString(other);
        return val.equals(otherVal);
    }

    public boolean equals(Object other)
    {
        return false;
    }

    @Override
    public String toString()
    {
        return val;
    }

}
