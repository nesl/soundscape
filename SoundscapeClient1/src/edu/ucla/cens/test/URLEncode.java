package edu.ucla.cens.test;

public class URLEncode {
	
	/**
	 * This URL encodes a string s and returns the result.
	 * Code was taken from:
	 * http://forum.java.sun.com/thread.jspa?threadID=341790&messageID=1408555
	 * 
	 * @param s is a String. If it's null, then the result is null.
	 * @return a string that represents s URL encoded. If s is null, then the
	 * result is null.
	 */
	static String encode(String s)
	{
		if (s!= null)
		{
			StringBuffer tmp = new StringBuffer();
			int i = 0;
			try 
			{
				while (true)
				{
					int b = (int)s.charAt(i++);
					if ((b >= 0x30 && b <= 0x39) 
						|| (b >= 0x41 && b <= 0x5A)
						|| (b >= 0x61 && b <= 0x7A))
					{
						tmp.append((char)b);
					}
					else
					{
						tmp.append("%");
						if (b <= 0xf)
						{
							tmp.append("0");
						}
						tmp.append(Integer.toHexString(b));
					}
				}
			}
			catch (Exception e) {}
			return tmp.toString();
		}
		return null;
	}
}
