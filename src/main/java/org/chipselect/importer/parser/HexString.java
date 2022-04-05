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
        if(1 > value.length())
        {
            return "0x0";
        }
        else
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
        if((null == val) || (null == other))
        {
            return false;
        }
        else
        {
            String otherVal = other.toString();
            return val.equals(otherVal);
        }
    }

    public boolean equals(String other)
    {
        if(null == val)
        {
            return false;
        }
        else
        {
            HexString o = new HexString(other);
            String oVal = o.toString();
            return val.equals(oVal);
        }
    }

    public boolean equals(int other)
    {
        if(null == val)
        {
            return false;
        }
        else
        {
            String otherVal = longToString(other);
            return val.equals(otherVal);
        }
    }

    public boolean equals(long other)
    {
        if(null == val)
        {
            return false;
        }
        else
        {
            String otherVal = longToString(other);
            return val.equals(otherVal);
        }
    }

    public boolean equals(Object other)
    {
        return false;
    }

    private int char2Int(char c)
    {
        switch(c)
        {
        case '1': return 1;
        case '2': return 2;
        case '3': return 3;
        case '4': return 4;
        case '5': return 5;
        case '6': return 6;
        case '7': return 7;
        case '8': return 8;
        case '9': return 9;
        case 'a': return 10;
        case 'A': return 10;
        case 'b': return 11;
        case 'B': return 11;
        case 'c': return 12;
        case 'C': return 12;
        case 'd': return 13;
        case 'D': return 13;
        case 'e': return 14;
        case 'E': return 14;
        case 'f': return 15;
        case 'F': return 15;
        case '0':
        default:
            return 0;
        }
    }

    public HexString add(int value)
    {
        return this.add((long)value);
    }

    public HexString add(long value)
    {
        if(null == val)
        {
            return new HexString(value);
        }
        // val must now be like "0x1234"
        int posval = val.length() -1;
        int carry = 0;
        StringBuilder sb = new StringBuilder();
        while((value > 0) || (1 < posval))
        {
            int i = (int)(value%16); // i can not be larger than 15, so cast is not an issue
            value = value - i;
            value = value /16;
            char old;
            if(1 < posval)
            {
                old = val.charAt(posval);
                posval--;
            }
            else
            {
                // value has more positions than val has -> add leading zeros.
                old = '0';
            }
            int oi = char2Int(old);
            int cur = i + oi + carry;
            if(15 < cur)
            {
                carry = cur -16;
                sb.append(intToChar(carry));
                carry = 1;
            }
            else
            {
                sb.append(intToChar(cur));
            }
        }

        if(0 != carry)
        {
            sb.append(intToChar(carry));
        }
        sb.reverse();
        return new HexString("0x" + sb.toString());
    }


    @Override
    public String toString()
    {
        return val;
    }

}
